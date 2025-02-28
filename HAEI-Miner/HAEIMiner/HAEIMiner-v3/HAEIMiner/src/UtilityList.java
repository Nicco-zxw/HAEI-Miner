import java.util.ArrayList;
import java.util.List;

public class UtilityList {
    int item;//项目名称
    int mn = 0;
    long inv = 0;//项目总投资
    double Sumae = 0;//效益的总和


    List<Element> elements = new ArrayList<Element>();

    public UtilityList(int item) {this.item = item;}

   // 更新项目总投资
    public void addInvestion(long invOfItem){
        inv = invOfItem;
    }

    // 添加元素到列表
    public void addElement(Element element, int n) {
        Sumae += (double) element.iutils / inv;
        elements.add(element);
        if(mn < n) mn = n;
    }

    public int getMn(){return mn;}

    //获取项目总投资
    public long getInv(){
        return inv;
    }
}
