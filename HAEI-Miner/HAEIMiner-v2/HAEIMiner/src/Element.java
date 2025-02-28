class Element {
    final int tid;//事务ID
    final int iutils ;//项目效用值
    final int rmu;//项目剩余最大效用
    final long rMinInv;

    public Element(int tid, int iutils, int rmu,long rMinInv){
        this.tid = tid;
        this.iutils = iutils;
        this.rmu = rmu;
        this.rMinInv = rMinInv;
    }
}
