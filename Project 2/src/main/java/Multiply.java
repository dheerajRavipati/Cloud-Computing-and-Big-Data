import java.io.*;
import java.util.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.util.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;

class Pair implements WritableComparable<Pair> {
  public int i;
  public int j;

  Pair() {
  }

  Pair(int i, int j) {
    this.i = i;
    this.j = j;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    out.writeInt(i);
    out.writeInt(j);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    i = in.readInt();
    j = in.readInt();
  }

  @Override
  public int compareTo(Pair pair) {
    int val = Integer.compare(i, pair.i);

    if (val == 0) {
      val = Integer.compare(j, pair.j);
    }

    return val;
  }

  public String toString() {
    return i + "," + j + ",";
  }
}

class Elem implements Writable {
  public short tag;
  public int index;
  public double value;

  Elem() {
  }

  Elem(short tag, int index, double value) {
    this.tag = tag;
    this.index = index;
    this.value = value;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    out.writeShort(tag);
    out.writeInt(index);
    out.writeDouble(value);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    tag = in.readShort();
    index = in.readInt();
    value = in.readDouble();
  }
}

public class Multiply extends Configured implements Tool {

  // Variable to assign count of #rows in matrix M
  private static int Mr = 0;
  // Variable to assign count of #columns in matrix N
  private static int Nc = 0;

  // Mapper for matrix M
  public static class MatrixM extends Mapper<Object, Text, Pair, Elem> {

    public void map(Object key, Text value, Context context)
        throws IOException, InterruptedException {

      // Split line based on delimiter ","
      Scanner sc = new Scanner(value.toString()).useDelimiter(",");

      int i = sc.nextInt();
      int j = sc.nextInt();
      double v = sc.nextDouble();

      // emit elements for each column in N
      for (int k = 0; k < Nc; k++) {
        context.write(new Pair(i, k), new Elem((short) 0, j, v));
      }

      sc.close();
    }

  }

  // Mapper for matrix N
  public static class MatrixN extends Mapper<Object, Text, Pair, Elem> {

    public void map(Object key, Text value, Context context)
        throws IOException, InterruptedException {

      Scanner sc = new Scanner(value.toString()).useDelimiter(",");

      int j = sc.nextInt();
      int k = sc.nextInt();
      double v = sc.nextDouble();

      // emit elements for each row in M
      for (int i = 0; i < Mr; i++) {
        context.write(new Pair(i, k), new Elem((short) 1, j, v));
      }

      sc.close();
    }

  }

  // Reducer
  public static class MatrixReducer extends Reducer<Pair, Elem, Pair, DoubleWritable> {
    public void reduce(Pair key, Iterable<Elem> values, Context context)
        throws IOException, InterruptedException {

      Hashtable<Integer, Double> M = new Hashtable<>();
      Hashtable<Integer, Double> N = new Hashtable<>();

      // Add element value to matrix M if tag = 0 otherwise assign to matrix N if tag
      // = 1
      for (Elem element : values) {
        if (element.tag == 0) {
          M.put(element.index, element.value);
        } else {
          N.put(element.index, element.value);
        }
      }

      double result = 0.0;
      for (int j = 0; j < Mr; j++) {
        double element_m = M.containsKey(j) ? M.get(j) : 0.0;
        double element_n = N.containsKey(j) ? N.get(j) : 0.0;
        result += element_m * element_n;

      }
      context.write(key, new DoubleWritable(result));
    }
  }

  public int run(String[] args) throws Exception {

    // Count number of rows in matrix M
    File file = new File("M-matrix-small.txt");
    Scanner sc1 = new Scanner(file);
    while (sc1.hasNextLine()) {
      String line = sc1.nextLine();
      int x = Integer.parseInt(line.charAt(0) + "");
      if (Mr < x) {
        Mr = x + 1;
      }
    }
    sc1.close();

    // Count number of columns in Matrix N
    File file2 = new File("N-matrix-small.txt");
    Scanner sc2 = new Scanner(file2);
    while (sc2.hasNextLine()) {
      String line = sc2.nextLine();
      int x = Integer.parseInt(line.charAt(2) + "");
      if (Nc < x) {
        Nc = x + 1;
      }
    }
    sc2.close();

    // Set up Hadoop job
    Configuration conf = new Configuration();
    Job job = Job.getInstance(conf);
    job.setJobName("MapFinalOutput");
    job.setJarByClass(Multiply.class);
    job.setOutputKeyClass(Pair.class);
    job.setOutputValueClass(Elem.class);
    job.setReducerClass(MatrixReducer.class);
    MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, MatrixM.class);
    MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, MatrixN.class);
    job.setOutputFormatClass(TextOutputFormat.class);
    FileOutputFormat.setOutputPath(job, new Path(args[2]));

    return job.waitForCompletion(true) ? 0 : 1;
  }

  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new Configuration(), new Multiply(), args);
    System.exit(res);
  }
}