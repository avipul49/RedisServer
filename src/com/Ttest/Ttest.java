package com.Ttest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 测试2种矩阵类的性能
 * @author zhangpeng
 * @date 2013-5-28
 */
public class Ttest
{

	public static void main(String[] args)
	{
		test1(10000, 500);
		test2(10000, 500);
	}

	/**
	 * 测试MatrixBuffer的性能
	 * 
	 * @author zhangpeng
	 * @date 2013-5-28
	 * @param rows
	 * @param columns
	 */
	public static void test1(int rows, int columns)
	{

		MatrixBuffer buffer = new MatrixBuffer(rows, columns);

		float f = 1;

		float[] resultArray = new float[columns];

		for (int j = 0; j < resultArray.length; j++)
		{
			resultArray[j] = f;
			f++;
		}

		long time1 = System.currentTimeMillis();
		
		for (int i = 0; i < rows; i++)
		{
			buffer.addRow(resultArray);
		}
		
		long time2 = System.currentTimeMillis();

		System.out.println("MatrixBuffer addRow方法性能：" + (time2 - time1));

		long time3 = System.currentTimeMillis();
		float[][] gs = buffer.getArray(10000);
		long time4 = System.currentTimeMillis();

		System.out.println("MatrixBuffer getArray方法性能：" + (time4 - time3));

		// for (float[] gs2 : gs)
		// {
		// for (float g : gs2)
		// {
		// System.out.print(g+" ");
		// }
		// System.out.println();
		// }
	}

	/**
	 * 测试MatrixBuffer2的性能
	 * 
	 * @author zhangpeng
	 * @date 2013-5-28
	 * @param rows
	 * @param columns
	 */
	public static void test2(int rows, int columns)
	{

		MatrixBuffer2 buffer = new MatrixBuffer2(rows, columns);

		float f = 1;

		float[] resultArray = new float[columns];

		for (int j = 0; j < resultArray.length; j++)
		{
			resultArray[j] = f;
			f++;
		}

		long time1 = System.currentTimeMillis();
		
		for (int i = 0; i < rows; i++)
		{
			buffer.addRow(resultArray);
		}
		
		long time2 = System.currentTimeMillis();

		System.out.println("MatrixBuffer2 addRow方法性能：" + (time2 - time1));

		long time3 = System.currentTimeMillis();
		float[][] gs = buffer.getArray(10000);
		long time4 = System.currentTimeMillis();

		System.out.println("MatrixBuffer2 getArray方法性能：" + (time4 - time3));

//		 for (float[] gs2 : gs)
//		 {
//		 for (float g : gs2)
//		 {
//		 System.out.print(g+" ");
//		 }
//		 System.out.println();
//		 }
	}
}

/**
 * 限定长宽的矩阵（使用LinkedBlockingDeque）
 * 
 * @author zhangpeng
 * @date 2013-5-28
 */
class MatrixBuffer
{
	List<LinkedBlockingDeque<Float>> matrix;
	int number_rows;
	int number_columns;

	public MatrixBuffer(int _number_rows, int _number_columns)
	{
		number_rows = _number_rows;
		number_columns = _number_columns;
		matrix = new ArrayList<LinkedBlockingDeque<Float>>(number_columns);
		for (int i = 0; i < number_columns; i++)
		{
			LinkedBlockingDeque<Float> column = new LinkedBlockingDeque<Float>(
					number_rows);
			matrix.add(column);
		}
	}

	public LinkedBlockingDeque<Float> getColunmn(int index)
	{
		return matrix.get(index);
	}

	public float[] getFirstRow()
	{
		float[] firstRow = new float[number_columns];
		for (int i = 0; i < number_columns; i++)
		{
			LinkedBlockingDeque<Float> column = matrix.get(i);
			float result = column.peekLast();
			firstRow[i] = result;
		}
		return firstRow;
	}

	public void addRow(float[] resultArray)
	{
		for (int i = 0; i < number_columns; i++)
		{
			LinkedBlockingDeque<Float> column = matrix.get(i);
			float result = resultArray[i];
			if (column.size() == number_rows)
			{
				column.removeFirst();
			}
			column.add(result);
		}

	}

	/**
	 * 获取前几行的数据
	 * 
	 * @author zhangpeng
	 * @date 2013-5-28
	 * @param row
	 * @return
	 */
	public float[][] getArray(int row)
	{
		float[][] fs = new float[row][number_columns];

		for (int i = 0; i < number_columns; i++)
		{
			LinkedBlockingDeque<Float> blockingDeque = getColunmn(i);

			Object[] temp = blockingDeque.toArray();

			for (int j = 0; j < row; j++)
			{
				fs[row - 1 - j][i] = (Float) temp[j];
			}
		}

		return fs;
	}

}

/**
 * 限定长宽的矩阵（使用数组）
 * 
 * @author zhangpeng
 * @date 2013-5-28
 */
class MatrixBuffer2
{
	/** 矩阵的数据 */
	List<float[]> matrix;
	/** 矩阵的行数 */
	int number_rows;
	/** 矩阵的列数 */
	int number_columns;

	public MatrixBuffer2(int number_rows, int number_columns)
	{
		this.number_rows = number_rows;
		this.number_columns = number_columns;
		// 使用arrayList还是LinkedList根据使用情况而定，如果该类的读取操作较多，使用ArrayList， 如果插入操作较多使用LinkedList
		 matrix = new ArrayList<float[]>();
		//matrix = new LinkedList<float[]>();
	}
	
	//获取对应列数的方法我没写，我不知道你们为什么需要这个方法
//	 getColunmn(int index)

	/**
	 * 获取第一行
	 * 
	 * @author zhangpeng
	 * @date 2013-5-28
	 * @return
	 */
	public float[] getFirstRow()
	{
		return matrix.get(0);
	}

	/**
	 * 添加数据到第一行，如果行数超过限制，移除最后一行
	 * 
	 * @author zhangpeng
	 * @date 2013-5-28
	 * @param resultArray
	 * @throws Exception 
	 */
	public void addRow(float[] resultArray)
	{ 
		//如果数组长度大于矩阵列数，截取数组
		if (resultArray.length>number_columns)
		{
			float[] temp = new float[number_columns];
			System.arraycopy(resultArray, 0, temp, 0, number_columns);
			resultArray = temp;
		}
		
		matrix.add(0, resultArray);

		if (matrix.size() > number_rows)
		{
			matrix.remove(matrix.size() - 1);
		}
	}

	/**
	 * 获取前几行的数据
	 * 
	 * @author zhangpeng
	 * @date 2013-5-28
	 * @param row
	 * @return
	 */
	public float[][] getArray(int row)
	{
		//如果要取的行数大于矩阵现有的长度，则取出所有数据
		row = row>matrix.size()?matrix.size():row;
		
		float[][] fs = new float[row][number_columns];

		for (int i = 0; i < row; i++)
		{
			fs[i] = matrix.get(i);
		}

		return fs;
	}
}
