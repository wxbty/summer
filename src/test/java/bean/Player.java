package bean;

import ink.zfei.summer.beans.factory.annotation.Autowired;

import javax.annotation.Resource;

public class Player {

    @Resource
    private Area area;

    @Autowired
    private Person person;


    public void sourceArea() {
        if (person == null) {
            throw new RuntimeException("@Resource area ==null");
        }
        System.out.println(area);
    }

    public void autoPerson() {
        if (person == null) {
            throw new RuntimeException("@Autowired person ==null");
        }
        System.out.println(person);
    }
}
