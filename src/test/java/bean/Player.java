package bean;

import javax.annotation.Resource;

public class Player {

    @Resource
    private Area area;


    public void name() {
        System.out.println(area);
    }
}
