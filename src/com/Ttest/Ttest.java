package com.Ttest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * ����2�־����������
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
	 * ����MatrixBuffer������
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

		System.out.println("MatrixBuffer addRow�������ܣ�" + (time2 - time1));

		long time3 = System.currentTimeMillis();
		float[][] gs = buffer.getArray(10000);
		long time4 = System.currentTimeMillis();

		System.out.println("MatrixBuffer getArray�������ܣ�" + (time4 - time3));

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
	 * ����MatrixBuffer2������
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

		System.out.println("MatrixBuffer2 addRow�������ܣ�" + (time2 - time1));

		long time3 = System.currentTimeMillis();
		float[][] gs = buffer.getArray(10000);
		long time4 = System.currentTimeMillis();

		System.out.println("MatrixBuffer2 getArray�������ܣ�" + (time4 - time3));

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
 * �޶�����ľ���ʹ��LinkedBlockingDeque��
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
	 * ��ȡǰ���е�����
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
 * �޶�����ľ���ʹ�����飩
 * 
 * @author zhangpeng
 * @date 2013-5-28
 */
class MatrixBuffer2
{
	/** ��������� */
	List<float[]> matrix;
	/** ��������� */
	int number_rows;
	/** ��������� */
	int number_columns;

	public MatrixBuffer2(int number_rows, int number_columns)
	{
		this.number_rows = number_rows;
		this.number_columns = number_columns;
		// ʹ��arrayList����LinkedList����ʹ������������������Ķ�ȡ�����϶࣬ʹ��ArrayList�� �����������϶�ʹ��LinkedList
		 matrix = new ArrayList<float[]>();
		//matrix = new LinkedList<float[]>();
	}
	
	//��ȡ��Ӧ�����ķ�����ûд���Ҳ�֪������Ϊʲô��Ҫ�������
//	 getColunmn(int index)

	/**
	 * ��ȡ��һ��
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
	 * ������ݵ���һ�У���������������ƣ��Ƴ����һ��
	 * 
	 * @author zhangpeng
	 * @date 2013-5-28
	 * @param resultArray
	 * @throws Exception 
	 */
	public void addRow(float[] resultArray)
	{ 
		//������鳤�ȴ��ھ�����������ȡ����
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
	 * ��ȡǰ���е�����
	 * 
	 * @author zhangpeng
	 * @date 2013-5-28
	 * @param row
	 * @return
	 */
	public float[][] getArray(int row)
	{
		//���Ҫȡ���������ھ������еĳ��ȣ���ȡ����������
		row = row>matrix.size()?matrix.size():row;
		
		float[][] fs = new float[row][number_columns];

		for (int i = 0; i < row; i++)
		{
			fs[i] = matrix.get(i);
		}

		return fs;
	}
}
