import java.io.*;
import java.util.*;

public class AlgoHAEIMiner {
    double maxMemory = 0;//最大内存使用

    long startTimestamp = 0;//算法开始时间

    long endTimestamp = 0;//算法结束时间

    int haeiCount = 0;//高平均效益数量

    int joinCount = 0;//连接数

    double min_AEutility = 0;

    List<UtilityList> listOfUtilityLists = new ArrayList<UtilityList>();//定义效用列表
    Map<Integer,Long> mapItemToINV;//各项目的inv表
    Map<Integer,Double> mapItemToAEUB;//AEUB表
    Map<Integer,Long> mapItemToMu;//项目的mu表
    BufferedWriter writer = null;//定义写入数据的方式

    Map<Integer,Map<Integer,Double>> mapItemToEAES;

    //交易事务中的项目id与效用 类pair
    class Pair{
        int item = 0;
        int utility = 0;
        long rMinInv = 0;
    }

    public AlgoHAEIMiner(){
    }

    public void runAlgorithm(String input1, String intput2, String output, double min_AE) throws IOException{
       //用于获得当前系统时间，记为算法开始时间
        startTimestamp = System.currentTimeMillis();
        //定义写入数据的位置
        writer = new BufferedWriter(new FileWriter(output));
        //定义AEUB表的存储位置
        mapItemToAEUB = new HashMap<Integer,Double>();
        //定义投资值表的存储位置
        mapItemToINV = new HashMap<Integer,Long>();
        //定义mu表的存储位置即每个项目在每个事务中最大效用之和
        mapItemToMu = new HashMap<Integer,Long>();
        //定义估计共现结构存放位置
        mapItemToEAES = new HashMap<Integer,Map<Integer,Double>>();
        //定义阈值
        min_AEutility = min_AE;
        BufferedReader myInput = null;
        String thisLine;

        //扫描投资库，得到每个项目的投资值
        try{
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(intput2))));
            while ((thisLine = myInput.readLine()) != null) {
                if (thisLine.isEmpty() == true ||
                        thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@') {
                    continue;
                }
                String[] str = thisLine.split(" ");
                Integer item = Integer.valueOf(str[0]);
                Long invest = Long.parseLong(str[1]);
                mapItemToINV.put(item,invest);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(myInput != null){
                myInput.close();
            }
        }

        //第一次扫描数据库
        try {
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input1))));
            while ((thisLine = myInput.readLine()) != null){
                if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#') {
                    continue;
                }
                String[] split = thisLine.split(":");//分割
                String[] items = split[0].split(" ");//获取项目id
                String[] utilityValues = split[2].split(" ");//获取项目效用
                //求出事务中的最大效用值存入transactionMUtility
                Integer transactionMUtility = Integer.MIN_VALUE;//Integer.MIN_VALUE常量-2147483648
                for (int i = 0; i < utilityValues.length; i++) {
                    if (transactionMUtility < Integer.parseInt(utilityValues[i])) {
                        transactionMUtility = Integer.parseInt(utilityValues[i]);
                    }
                }

                //得到每个项目的MU表
                for(int i = 0; i < items.length; i++){
                    Integer item = Integer.parseInt(items[i]);
                    Long mu = mapItemToMu.get(item);
                    mu = (mu == null)?  transactionMUtility: mu + transactionMUtility;
                    mapItemToMu.put(item, mu);
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(myInput != null){
                myInput.close();
            }
        }

        //得到所有1项集的AEUB值
        for (Integer item : mapItemToMu.keySet()) {
            Double aeub = mapItemToAEUB.get(item);
            double newaeub = (double)mapItemToMu.get(item) / mapItemToINV.get(item);
            aeub = (aeub == null)? newaeub : newaeub + aeub;
            mapItemToAEUB.put(item, aeub);
        }

        //创造列表空间
        List<Integer> ItemLists = new ArrayList<Integer>();

        //利用AEUB值对项目进行修建
        for(Integer item : mapItemToAEUB.keySet()){
            if(mapItemToAEUB.get(item) >= min_AEutility){
                ItemLists.add(item);
            }
        }
        //将1项目集中的项目按照投资值进行升序排序
        Collections.sort(ItemLists, new Comparator<Integer>() {
            public int compare(Integer o1, Integer o2) {
                return mapItemToINV.get(o1).compareTo(mapItemToINV.get(o2));
            }
        });


        //创造存放效用列表的空间
        Map<Integer,UtilityList> mapItemToUtility = new HashMap<Integer,UtilityList>();

        //将INV值存入对于的mapItemToUtility中
        for(Integer item : ItemLists){
            UtilityList utilityList = new UtilityList(item);
            utilityList.addInvestion(mapItemToINV.get(item));
            mapItemToUtility.put(item, utilityList);
        }

        //第二次扫描数据库
        try {
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input1))));
            //事务ID
            int tid = 0;
            while ((thisLine = myInput.readLine()) != null) {
                if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#') {
                    continue;
                }
                String[] split = thisLine.split(":");//分割
                String[] items = split[0].split(" ");//获取项目id
                String[] utilityValues = split[2].split(" ");//获取项目效用

                //记录当前事务的最大值
                double maxUtilityOfCurrentTranscation = 0;

                //定义列表存放用来存放revisedTransaction
                List<Pair> revisedTransaction = new ArrayList<Pair>();
                //遍历每个项目
                for (int i = 0; i < items.length; i++){
                    //定义pair类
                    Pair pair = new Pair();
                    //将项目id存入pair类中
                    pair.item = Integer.parseInt(items[i]);
                    //将项目效用存入pair类中
                    pair.utility = Integer.parseInt(utilityValues[i]);
                    //将项目投资值存入pair类中
                    pair.rMinInv = mapItemToINV.get(pair.item);
                    //如果pair类中的项目tid对应的aeub值大于最小阈值，则将值存入revisedTransaction
                    if(mapItemToAEUB.get(pair.item) >= min_AEutility){
                        revisedTransaction.add(pair);
                        if(maxUtilityOfCurrentTranscation < pair.utility){
                            maxUtilityOfCurrentTranscation = pair.utility;
                        }
                    }
                }

                //将revisedTransaction按照INV值进行升序排序
                Collections.sort(revisedTransaction, new Comparator<Pair>() {
                    public int compare(Pair o1, Pair o2) {
                        return mapItemToINV.get(o1.item).compareTo(mapItemToINV.get(o2.item));
                    }
                });

                //遍历修正后的事务
                for(int i = 0;i < revisedTransaction.size(); i++){
                    Pair pair = revisedTransaction.get(i);
                    //求出最大剩余效用和最小剩余投资值
                    int rmu = 0;
                    long rmi = pair.rMinInv;
                    for(int j = i+1; j < revisedTransaction.size(); j++){
                        Pair pair1 = revisedTransaction.get(j);
                       if(pair1.utility > rmu) rmu = pair1.utility;
                       if(pair1.rMinInv < rmi) rmi = pair1.rMinInv;
                    }

                    //剩余项目的个数
                    int mn = revisedTransaction.size() - 1 - i;
                    //获得对应项目对应的UtilityList
                    UtilityList utilityListOfItem = mapItemToUtility.get(pair.item);


                    //将Element值存入对应的列表中
                    Element element = new Element(tid, pair.utility, rmu, rmi,mn);
                    utilityListOfItem.addElement(element,mn);
                    //构建EAES去存放2项目集的AEUB
                    Map<Integer,Double> mapEAESItem = mapItemToEAES.get(pair.item);
                    //如未存在则新建一个空间
                    if(mapEAESItem == null){
                        mapEAESItem = new HashMap<Integer,Double>();
                        mapItemToEAES.put(pair.item,mapEAESItem);
                    }
                    //创建每一个ab，ac，ad的EAES
                    for(int j = i+1;j < revisedTransaction.size();j++){
                        Pair pairAfter = revisedTransaction.get(j);
                        Double invOfItem =(double)(mapItemToINV.get(pair.item) + mapItemToINV.get(pairAfter.item));
                        Double aeubSum = mapEAESItem.get(pairAfter.item);
                        if(aeubSum == null) {
                            mapEAESItem.put(pairAfter.item, maxUtilityOfCurrentTranscation / invOfItem);
                        }else {
                            mapEAESItem.put(pairAfter.item,aeubSum + maxUtilityOfCurrentTranscation / invOfItem);
                        }
                    }
                }
                tid++;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(myInput != null){
                myInput.close();
            }
        }

        //将mapItemToUtility存入listOfUtilityLists中
        listOfUtilityLists.addAll(mapItemToUtility.values());

        //将列表中的元素按照inv值升序排序
        Collections.sort(listOfUtilityLists, new Comparator<UtilityList>() {
            public int compare(UtilityList o1, UtilityList o2) {
                return mapItemToINV.get(o1.item).compareTo(mapItemToINV.get(o2.item));
            }
        });

        //检查内存使用量
        checkMemory();
        //HAEIMiner
        HAEIMine(new int[0],null,listOfUtilityLists, min_AEutility);
        //检查内存使用量
        checkMemory();
        //关闭输出文件
        writer.close();
        //算法结束时间
        endTimestamp = System.currentTimeMillis();
    }


    //搜索挖掘算法HAEIMine
    private void HAEIMine(int[] prefix,UtilityList pUL,List<UtilityList> ULs,double min_AE)
            throws IOException {
        //遍历P的所有扩展项
        for (int i = 0; i < ULs.size(); i++){
            UtilityList X = ULs.get(i);
            int length = prefix.length +1;
            //如果X的平均效益大于最小阈值，则输出X
            if (X.Sumae /length >= min_AE){
                //输出X到文件中
                writeOut(prefix, X.item ,X.Sumae /length);
            }
            //计算上界
            double Mae;
            if(X.mn == 0) Mae = 0;
            else{
                Mae = calMae(X, length);
            }
            //如果扩展项有存在成为高价值目标的可能性，继续扩展
            if (Mae >= min_AE){
                List<UtilityList> exULs = new ArrayList<UtilityList>();
                //遍历X之后的所有元素
                for (int j = i+1; j < ULs.size(); j++){
                    UtilityList Y = ULs.get(j);
                    //EAES-Prune
                    Map<Integer,Double> mapEAES = mapItemToEAES.get(X.item);
                    if(mapEAES != null) {
                        Double aeubY = mapEAES.get(Y.item);
                        if (aeubY == null || aeubY < min_AEutility) {
                            continue;
                        }
                    }
                    exULs.add(construct(pUL,X,Y));
                }
                //创建新的前缀
                int [] newPrefix = new int[prefix.length+1];
                //扩展
                System.arraycopy(prefix, 0, newPrefix, 0, prefix.length);
                newPrefix[prefix.length] = X.item;
                //递归调用HAEIMine
                HAEIMine(newPrefix, X, exULs, min_AE);
            }
        }
    }

    private double calMae(UtilityList ul, int length){
        double sumMau = 0;

        for(int i = 0;i<ul.elements.size();i++){
            Element element = ul.elements.get(i);
            int au = element.iutils/length;
            double tmae;
            //TODO
            double invNew = (double)(ul.inv + element.rMinInv);
            if (element.rmu != 0) {
                if(element.rmu > au) {
                    tmae = (double) (element.iutils + element.rmu * element.nofitem) / (invNew * (length + element.nofitem));
                }else {
                    tmae = (double) (element.iutils + element.rmu )/(invNew * (length + 1));
                }
            }
            else {
                tmae = 0;
            }

            sumMau = sumMau + tmae;

        }
        return sumMau;
    }


    //构建扩展项Pxy的效用列表
    private UtilityList construct(UtilityList P,UtilityList px,UtilityList py){
        //定义一个空列表
        UtilityList pxyUL = new UtilityList(py.item);
        long invOfItem = 0;
        //计算合并的总投资值，并填入到pxyUL中
        if(P == null) {
             invOfItem = px.getInv() + py.getInv();
        }else{
             invOfItem = px.getInv() + py.getInv() - P.getInv();
        }
        pxyUL.addInvestion(invOfItem);
        //遍历Px中的元素
        for(Element ex : px.elements){
            //使用二进制搜索来寻找py中与px有重合的tid
            Element ey =  findElementWithTID(py, ex.tid);
            if (ey == null){
                continue;
            }
            //如果没有前缀项的情况
            if (P == null){
                //填入新的效用列表中
                Element eXY = new Element(ex.tid, ex.iutils + ey.iutils, ey.rmu, ey.rMinInv,ey.nofitem);
                pxyUL.addElement(eXY,py.getMn());
            }else{
                Element e = findElementWithTID(P, ex.tid);
                //有前缀项的情况
                if (e != null) {
                    //填入新的效用列表中
                    Element eXY = new Element(ex.tid, ex.iutils + ey.iutils - e.iutils, ey.rmu, ey.rMinInv,ey.nofitem);
                    pxyUL.addElement(eXY, py.getMn());
                }
            }
        }
        //记录链接操作数
        joinCount++;
        //返回Pxy的列表信息
        return pxyUL;
    }


    //执行二进制搜索以在实用程序列表中查找具有给定 tid 的元素
    private Element findElementWithTID(UtilityList ulist, int tid){
        //获取元素列表
        List<Element> list = ulist.elements;
        //二分搜索
        int first = 0;
        int last = list.size() - 1;
        while( first <= last ) {
            int middle = ( first + last ) >>> 1;
            if(list.get(middle).tid < tid){
                first = middle + 1;
            }
            else if(list.get(middle).tid > tid){
                last = middle - 1;
            }
            else{
                return list.get(middle);
            }
        }
        return null;
    }


    //将高平均效益项集写入文件
    private void writeOut(int[] prefix, int item, double ae) throws IOException {
        //增加高平均效益数量
        haeiCount++;
        //创建一个StringBuffer
        StringBuffer buffer = new StringBuffer();
        //遍历前缀
        for (int i = 0; i < prefix.length; i++) {
            buffer.append(prefix[i]);
            buffer.append(' ');
        }
        //添加项目
        buffer.append(item);
        // 添加平均效益值
        buffer.append(" #AE: ");
        buffer.append(ae);
        //写入String
        writer.write(buffer.toString());
        // 写入新的一行
        writer.newLine();
    }


    //检查算法内存使用量
    private void checkMemory() {
        // 得到当前内存使用量
        double currentMemory = (Runtime.getRuntime().totalMemory() -  Runtime.getRuntime().freeMemory())
                / 1024d / 1024d;
        // 如果当前内存使用量大于最大内存使用量，则更新最大内存使用量
        if (currentMemory > maxMemory) {
            //更新最大内存使用量
            maxMemory = currentMemory;
        }
    }

    //输出算法运行状态
    public void printStats() {
        System.out.println("=============  HAEI-MINER ALGORITHM =============");
        System.out.println(" The final minutil : " + min_AEutility);
        System.out.println(" High average-efficiency itemsets count : " + haeiCount);
        System.out.println(" Total time ~ " + (endTimestamp - startTimestamp) + " ms");
        System.out.println(" Memory ~ " + maxMemory+ " MB");
        System.out.println(" Join count : " + joinCount);
        System.out.println("===================================================");
    }
}
