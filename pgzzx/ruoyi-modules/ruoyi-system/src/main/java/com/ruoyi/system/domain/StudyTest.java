package com.ruoyi.system.domain;

import java.util.Arrays;



    /**
     * @Description：
     * @Author: zhoujp
     * @CreateDate：2025-07-27
     * @Version：1.0
     */
    public class StudyTest {

        public static void main(String[] args) {
//        int[] nums = new int[]{2,4,6,7,4,6};
//        int[] nums = new int[]{0,1,0,3,2,3};
//        int length = getLength(nums);
//        System.out.println(length);
            soutArr();
        }

        /**
         * 第一题
         * @param nums
         * @return
         */
        public static int getLength(int[] nums){
            if (nums== null || nums.length ==0) {
                return 0;
            }
            int[] dp = new int[nums.length];
            Arrays.fill(dp,1);
            int maxLength = 0;
            for (int i = 1; i < nums.length; i++) {
                for (int j = 0; j < i; j++) {
                    if (nums[i] > nums[j]) {
                        dp[i] = Math.max(dp[j]+1, dp[i]);
                    }
                }
                maxLength = Math.max(maxLength, dp[i]);
            }
            return maxLength;
        }

        public static void soutArr() {
            int[] ints1 = {1, 2, 3, 4, 5};
            int[] ints2 = {6, 7, 8, 9, 10};
            int[] ints3 = {11,12,13,14,15};
            //3*5

            int[][] nums = {ints1,ints2,ints3};
            int[] res = new int[15];
            for (int i = 0; i < 5; i++) {
                for (int j = i + 1; j < 3; j++) {

                    System.out.println(nums[j][i]);
                }
            }
//            System.out.println(res);
        }
    }

