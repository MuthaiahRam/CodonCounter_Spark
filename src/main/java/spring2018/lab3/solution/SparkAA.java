package spring2018.lab3.solution;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import scala.Tuple2;

import org.apache.hadoop.io.Text;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.Function;

public class SparkAA {
	static Map<String, String> codon2aaMap = new HashMap<String, String>();

	public static void main(String[] args) throws Exception {
		// check args
		if (args.length != 3) {
			System.err.println("usage: SparkAA <input-file> <output-dir> <codon-table>");
			System.exit(1);
		}
		String inputFile = args[0];
		String outputDir = args[1];
		String codonTableFileName = args[2];

		// read in codon table into hash map
		codon2aaMap = readFile(codonTableFileName);

		// Create Java Spark Context
		SparkConf conf = new SparkConf().setAppName("sparkAA");
		SparkContext spark = new SparkContext(conf);

		// Load input data
		JavaRDD<String> input = spark.textFile(inputFile, 1).toJavaRDD();

		// get counts for each reading frame
		JavaPairRDD<String, Integer> RF1aaCounts = getCounts(input, 0);
		JavaPairRDD<String, Integer> RF2aaCounts = getCounts(input, 1);
		JavaPairRDD<String, Integer> RF3aaCounts = getCounts(input, 2);

		// filter out the 0-counts -- output file should only have non-0 records
		if(RF1aaCounts!=null){
		JavaPairRDD<String, Integer> RF1nonzero = getNonZeroRDD(RF1aaCounts);
		RF1nonzero.saveAsTextFile(outputDir + "_RF1");
		}
		if(RF2aaCounts!=null){
		JavaPairRDD<String, Integer> RF2nonzero = getNonZeroRDD(RF2aaCounts);
		RF2nonzero.saveAsTextFile(outputDir + "_RF2");
		}
		if(RF3aaCounts!=null){
		JavaPairRDD<String, Integer> RF3nonzero = getNonZeroRDD(RF3aaCounts);
		RF3nonzero.saveAsTextFile(outputDir + "_RF3");
		}

		spark.stop();
	}

	public static JavaPairRDD<String, Integer> getNonZeroRDD(JavaPairRDD<String, Integer> input) {
		JavaPairRDD<String, Integer> RFnonzero = input.filter(new Function<Tuple2<String,Integer>,Boolean>(){
			private static final long serialVersionUID = 1L;
			   public Boolean call(Tuple2<String, Integer> v1) throws Exception {
			   return !v1._2.equals(0);
			   }
			});
		
		// TODO use the RDD filter() function to filter out the records with 0
		// count
		// TODO return the RDD that contains only non-zero counts
		
		return RFnonzero;
	}

	public static JavaPairRDD<String, Integer> getCounts(JavaRDD<String> input, final int readingFrame) {
		// TODO use the flatMap() function to tokenize the record for the
		// reading frame -- 0, 1, or 2
		// TODO create codons of 3 nucleotides for the tokenized record and add
		// to a list
		// TODO return the list iterator
		JavaRDD<String> RFwords = input.flatMap(new FlatMapFunction<String, String>() {
			private static final long serialVersionUID = 1L;

			public Iterator<String> call(String x) {

				ArrayList<String> store = new ArrayList<String>();
				//if (!x.startsWith(">")) {
					if (readingFrame == 0) {

						for (int i = 0; i <= x.length() - 3; i = i + 3) {
							store.add(x.substring(i, i + 3));

						}

					}
					else if (readingFrame == 1){
						StringBuffer bufferFrameTwo = new StringBuffer(x).deleteCharAt(0);
						String frame2 = bufferFrameTwo.toString();
						for (int i = 0; i <=frame2.length() - 3; i = i + 3) {
							store.add(frame2.substring(i, i + 3));
						}
						
					}
					else{
						String bufferFrameThree = x.substring(2, x.length());
						String frame3 = bufferFrameThree.toString();
						for (int i = 0; i <=frame3.length() - 3; i = i + 3) {
							store.add(frame3.substring(i, i + 3));
						}
					}
				//} 
					return store.iterator();
			}
		});

		// TODO use the mapToPair() method on RFwords
		// TODO look up each codon in the codon2aaMap and return (amino acid, 0
		// or 1)
		JavaPairRDD<String, Integer> RFcodonCounts = RFwords.mapToPair(
	            new PairFunction<String, String, Integer>() {
	                private static final long serialVersionUID = 1L;
	                public Tuple2<String, Integer> call(String s) {
	                	String chromosome=codon2aaMap.get(s);
	                	if(chromosome==null){
	                    return new Tuple2<String, Integer>(chromosome, 0);
	                }
	                	else{
	                		return new Tuple2<String, Integer>(chromosome, 1);
	                	}
	            }});

		// TODO use the reduceByKey() method on RFcodonCounts
		// TODO do pairwise addition and return the sum
		JavaPairRDD<String, Integer> RFaaCounts = RFcodonCounts.reduceByKey(
	            new Function2<Integer, Integer, Integer>(){
	            	private static final long serialVersionUID = 1L;
	                public Integer call(Integer x, Integer y){ return x + y; }
	            } );


		return RFaaCounts;
	}

	protected static HashMap<String, String> readFile(String codonFilePath) {
		HashMap<String, String> codonMap = new HashMap<String, String>();
		BufferedReader cacheReader = null;
		String line = null;
		String[] lineArray = null;
		try {
			cacheReader = new BufferedReader(new FileReader(codonFilePath));
			while ((line = cacheReader.readLine()) != null) {
				// Isoleucine I ATT, ATC, ATA
				lineArray = line.split("\\t");
				String aminoAcid = lineArray[0];
				String[] sequencesArray = lineArray[2].split(",");
				for (String sequence : sequencesArray) {
					codonMap.put(sequence.trim(), aminoAcid.trim());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return codonMap;
	}
}
