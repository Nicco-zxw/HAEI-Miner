class Element {
    final int tid;//事务ID
    final int iutils ;//项目效用值
    final int rmu;//项目剩余最大效用
    final long rMinInv;//剩余事务最小投资值
    final int nofitem;//事务剩余项目数量

    public Element(int tid, int iutils, int rmu,long rMinInv,int nofitem){
        this.tid = tid;
        this.iutils = iutils;
        this.rmu = rmu;
        this.rMinInv = rMinInv;
        this.nofitem = nofitem;
    }
}
