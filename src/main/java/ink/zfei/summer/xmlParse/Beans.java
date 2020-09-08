package ink.zfei.summer.xmlParse;

import java.util.ArrayList;
import java.util.List;

public class Beans {
    public List<Bean> getBeanList() {
        return beanList;
    }

    List<Bean> beanList = new ArrayList<>();

    public void addNode(Bean bean) {
        beanList.add(bean);
    }

}
