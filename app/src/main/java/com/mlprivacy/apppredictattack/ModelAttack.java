package com.mlprivacy.apppredictattack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


//模型逆向攻击
public class ModelAttack
{
    static class PredictResultReceiver extends BroadcastReceiver{

        MyCallback callback;

        @Override
        public void onReceive(Context context, Intent intent) {
            String resultData = getResultData();
            Log.e("test", "预测完成，预测结果为 " + resultData);
            if (callback != null) {
                callback.success(resultData); //TODO::处理预测结果
            }
            abortBroadcast();
        }
    }

    interface MyCallback {
        void success(String result);
    }

    private PredictResultReceiver predictResultReceiver;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void init() {
        predictResultReceiver = new PredictResultReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.mlprivacy.apppredictattack.predictAction");
        intentFilter.setPriority(1);
        BaseApplication.appContext.registerReceiver(predictResultReceiver, intentFilter, Context.RECEIVER_EXPORTED);
    }

    public void release() {
        BaseApplication.appContext.unregisterReceiver(predictResultReceiver);
    }

    public void sendPredictRequest(MyCallback callback){
        predictResultReceiver.callback = callback;
        Intent intent = new Intent("com.mlprivacy.apppredictattack.predictAction");
        intent.putExtra("condition", new int[]{1, 2, 3, 4, 5});
        BaseApplication.appContext.sendOrderedBroadcast(intent, null);
    }

    /***
     * 攻击模块
     */
    public static void attack(String trainFilePath, String testFilePath, int target, int maxFeatures, int targetNum)
    {
        List<double[]> trainFeatureList = new ArrayList<>(); // 训练集特征
        List<double[]> testFeatureList = new ArrayList<>(); // 训练集特征
        List<Integer> testLabelList = new ArrayList<>(); // 训练集标签
        List<Integer> predictLabelList = new ArrayList<>(); // 预测结果标签

        // 读取训练集和测试集
        try
        {
            // 读取训练集特征
            InputStream inputStream = FileUtils.INSTANCE.readAssetFile(trainFilePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine();

            while (line != null)
            {
                double[] trainFeat = new double[maxFeatures];
                String[] lineFeat = line.split(" ");
                for (int i = 0; i < lineFeat.length; i++)
                {
                    trainFeat[i] = Double.parseDouble(lineFeat[i]);
                }
                trainFeatureList.add(trainFeat);

                line = reader.readLine();
            } // 行读取结束

            // 读取测试集特征和标签
            inputStream = FileUtils.INSTANCE.readAssetFile(testFilePath);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            line = reader.readLine();

            while (line != null)
            {
                double[] testFeat = new double[maxFeatures];
                String[] lineFeat = line.split(" ");
                for (int i = 0; i < lineFeat.length - 1; i++)
                {
                    testFeat[i] = Double.parseDouble(lineFeat[i]);
                }
                testFeatureList.add(testFeat);
                testLabelList.add(Integer.parseInt(lineFeat[lineFeat.length - 1]));

                line = reader.readLine();
            } // 行读取结束
        } catch (IOException e)
        {
            e.printStackTrace();// 文件读取结束
        }

        int totalNumLabels = getTotalNumLabels(); // App标签个数

        //边界分布，常驻地预测没有用到
        HashMap<Integer, Float> margs = getMargs(targetNum, target, trainFeatureList);
        int maxFeature = 0;
        float max = 0;
        Float m;
        for (int i = 0; i < targetNum; i++)
        {
            m = margs.get(i);
            assert m != null;
            if (m > max)
            {
                max = m;
                maxFeature = i;
            }
        }

        // 攻击预测值
        List<Integer> guesses = blackTreeInversionResidencePlaces(targetNum, testFeatureList, target, testLabelList, margs, maxFeatures, totalNumLabels, maxFeature);

        // 目标特征的实际值
        List<Integer> truth = getTruth(testFeatureList, target);

        //反预测精度
        float inversePrecision = getPrecision(truth, guesses);
        System.out.printf("特征常驻地的反预测准确率为：%.2f%%\n", inversePrecision * 100);
    }


    /**
     * 调用随机森林模型的预测模块
     */
    public static List<Double> getPredictDist()
    {
        /*
        // 原有的调用模型预测方法
        rf.predictLabel(X.get(i), totalNumLabels);
        List<Double> predictDist = rf.getPredictDist();
         */
        List<Double> predictDist = new ArrayList<>();
        for (int j = 0; j < 100; j++)
        {
            predictDist.add(1.0);
        }

        return predictDist;
    }

    /**
     *
     * @return 返回App标签个数
     */
    public static int getTotalNumLabels()
    {
        // 读取随机森林模型rf
        // int totalNumLabels = rf.getTotalNumLabels;
        int totalNumLabels = 58;

        return totalNumLabels;
    }


    public static HashMap<Integer, Float> getMargs(Integer n, Integer target, List<double[]> train_data)
    //n:总共有n类app target：目标特征在数组中为第几个特征 train_data:训练集
    {
        HashMap<Integer, Float> margs = new HashMap<>();
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < train_data.size(); i++)
        {
            l.add((int) train_data.get(i)[target]);
        }
        for (int i = 0; i < n; i++)
        {
            margs.put(i, (float) count(l, i) / (float) (l.size())); //某app出现次数除以总数
        }

        return margs;
    }


    //n:总共有n类app trainFeatureList:训练集 stats:概率分布 target:目标特征在数组中为第几个特征 y_test:测试集实际当前打开的app margs：边界分布
    public static List<Integer> blackTreeInversionResidencePlaces(Integer targetNum, List<double[]> trainFeatureList, Integer target, List<Integer> y_test, HashMap<Integer, Float> margs, int maxFeatures, int totalNumLabels, int maxfeature)

    {
        List<Integer> guesses = new ArrayList<>();
        for (int i = 0; i < trainFeatureList.size(); i++)
        {
            HashMap<Integer, Float> Y = feasibleTargetResidencePlaces(targetNum, trainFeatureList.get(i), target, y_test.get(i), maxFeatures, totalNumLabels);
            //System.out.println(Y);
            //System.out.println(trainFeatureList.get(i)[target]);
            int guess = predictTarget(Y, maxfeature);
            guesses.add(guess);
        }
        return guesses; //返回预测值集合
    }

    //获取目标特征的实际值
    public static List<Integer> getTruth(List<double[]> trainFeatureList, Integer target)
    //trainFeatureList:训练集 target:目标特征在数组中为第几个特征
    {
        List<Integer> truth = new ArrayList<>();
        for (int i = 0; i < trainFeatureList.size(); i++)
        {
            truth.add((int) trainFeatureList.get(i)[target]);
        }
        return truth;
    }

    //获取攻击精度
    public static float getPrecision(List<Integer> truth, List<Integer> guesses)
    //truth:目标特征实际值集合 guesses:目标特征反预测值集合
    {
        int count = 0;
        int count0 = 0;
        int count1 = 0;
        int count2 = 0;
        for (int i = 0; i < truth.size(); i++)
        {
//        	System.out.print(truth.get(i)+" ");
//          System.out.println(guesses.get(i));
            if (Objects.equals(truth.get(i), guesses.get(i)))
            {
                count += 1;
                if (truth.get(i) == 0)
                    count0 += 1;
                else if (truth.get(i) == 1)
                    count1 += 1;
                else
                    count2 += 1;
            }
        }

        /*
        Float precision0 = (float) count0 / (float) count(truth, 0);
        Float precision1 = (float) count1 / (float) count(truth, 1);
        Float precision2 = (float) count1 / (float) count(truth, 2);
//  		System.out.println(precision0);
//  		System.out.println(precision1);
//  		System.out.println(precision2);
         */

        return (float) count / truth.size(); //返回反预测精度

    }


    public static HashMap<Integer, Float> feasibleTargetResidencePlaces(Integer targetNum, double[] row, Integer target, Integer classfeat, int maxFeatures, int totalNumLabels)
    //n:总共有n类app row:输入测试集中的一条数据 stats:概率分布 target:目标特征在数组中为第几个特征 classfeat:实际当前打开的app margs:边界分布
    {
        List<double[]> X = new ArrayList<>();
        for (int i = 0; i < targetNum; i++)
        {
            double[] x = new double[maxFeatures];
            System.arraycopy(row, 0, x, 0, row.length);
            x[target] = (double) i;
            X.add(x); //构建所有可行向量存储在X中
        }
        HashMap<Integer, Float> Y = new HashMap<>();
        for (int i = 0; i < targetNum; i++)
        {
            // 预测调用的空方法
            List<Double> predictDist = getPredictDist();

            float predict = predictDist.get(classfeat).floatValue();
            Y.put(i, predict);
        }
        return Y;
    }




    //计算某数组中元素之和
    public static Integer sum(ArrayList<Integer> a)
    {
        int sum = 0;
        for (int i = 0; i < a.size(); i++)
        {
            sum = sum + a.get(i);
        }
        return sum;
    }

    //获取数组中某个元素的个数
    public static Integer count(List<Integer> a, Integer b)
    {
        int count = 0;
        for (int i = 0; i < a.size(); i++)
        {
            if (Objects.equals(b, a.get(i)))
            {
                count += 1;
            }
        }
        return count;

    }

    //获取APP的边界分布
    public static HashMap<Integer, Float> getMargsAppOne(Integer n, Integer target, ArrayList<double[]> train_data)
    //n:总共有n类app target：目标特征在数组中为第几个特征 train_data:训练集
    {
        HashMap<Integer, Float> margs = new HashMap<>();
        ArrayList<Integer> l = new ArrayList<>();
//        for (int i = 0; i < train_data.size(); i++)
//        {
//            l.add((int) train_data.get(i)[target]);
//        }
        for (int i = 0; i < train_data.size(); i++)
        {
            l.add((int) train_data.get(i)[target + 1]);
        }
        for (int i = 0; i < train_data.size(); i++)
        {
            l.add((int) train_data.get(i)[target + 2]);
        }
        for (int i = 0; i < n; i++)
        {
            margs.put(i, (float) count(l, i) / (float) (l.size())); //某app出现次数除以总数
        }

        return margs;
    }



    //构建混淆矩阵，获取模型预测的概率分布
    public static HashMap<Integer, HashMap<Integer, Float>> getPerfStats(ArrayList<Integer> y_train, ArrayList<Integer> train_est, Integer n)
    //y_train:训练集中当前打开的app实际值 train_est:将训练集输入到随机森林中获得的对于当前打开app的预测值 n:总共有n类app
    {
        HashMap<Integer, HashMap<Integer, Float>> stats = new HashMap<>();
        ArrayList<ArrayList<Integer>> CMatrix = new ArrayList<>();

        for (int i = 0; i < n; i++)
        {
            ArrayList<Integer> l = new ArrayList<>();
            for (int j = 0; j < n; j++)
            {
                l.add(0);
            }
            CMatrix.add(l);     //先构建一个n*n的0矩阵
        }
        //System.out.println(CMatrix);
        for (int i = 0; i < y_train.size(); i++)
        {
            int j = CMatrix.get(y_train.get(i)).get(train_est.get(i));
            CMatrix.get(y_train.get(i)).set(train_est.get(i), j + 1); //遍历所有结果，将矩阵中对应位置的值加1
        }
        for (int i = 0; i < n; i++)
        {
            HashMap<Integer, Float> statsi = new HashMap<>();
            for (int j = 0; j < n; j++)
            {
                if (sum(CMatrix.get(i)) == 0)
                {
                    float k = 0;
                    statsi.put(j, k);
                } else
                {
                    statsi.put(j, (float) CMatrix.get(i).get(j) / (float) sum(CMatrix.get(i))); //构建实际值在预测值上的概率分布
                }
            }
            stats.put(i, statsi);
        }
        return stats;
    }

    //给出概率最大的预测值
    public static Integer predictTarget(HashMap<Integer, Float> Y, int maxfeature)
    //Y：appone为各类app的概率集合
    {
        //System.out.println(Y);
        int guess = 0;
        //ArrayList<Integer> guess=new ArrayList<>();
        float max = 0;
        for (int i = 0; i < Y.size(); i++)
        {
            Float yFloat = Y.get(i);
            if (yFloat != null)
            {
                if (yFloat > max)
                {
                    guess = i;
                    max = yFloat;
                }
            }

        }

        Float yFloat = Y.get(maxfeature);
        if (yFloat != null)
        {
            if (max == yFloat)
            {
                guess = maxfeature;
            }
        }


        return guess;//返回预测值
    }

    public static ArrayList<Integer> predictTargetAppOne(HashMap<Integer, Float> Y)
    {
        ArrayList<Integer> guess = new ArrayList<>();
        for (int j = 0; j < 5; j++)
        {
            float max = 0;
            int guessx = 0;
            for (int i = 0; i < Y.size(); i++)
            {
                Float yFloat = Y.get(i);
                if (yFloat != null)
                {
                    if (yFloat > max)
                    {
                        guessx = i;
                        max = yFloat;
                    }
                }


            }
            guess.add(guessx);
            Y.replace(guessx, (float) 0);
        }
        return guess;
    }


    public static Float getPrecisionAppOne(ArrayList<Integer> truth, ArrayList<ArrayList<Integer>> guesses)
    //truth:目标特征实际值集合 guesses:目标特征反预测值集合
    {
        int count = 0;
        int count0 = 0;
        int count1 = 0;
        for (int i = 0; i < truth.size(); i++)
        {
            int flag = 0;
            for (int j = 0; j < 5; j++)
            {
                if (Objects.equals(truth.get(i), guesses.get(i).get(j)))
                {
                    flag = 1;
                    break;
                }
            }
            if (flag == 1)
            {
                count += 1;
            }
        }

        Float precision = (float) count / (float) truth.size();
        return precision; //返回反预测精度

    }

}