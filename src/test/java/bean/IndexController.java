package bean;

import ink.zfei.summer.annation.Component;

@Component
public class IndexController {

    private Person person;
    private Lion lion;

    public IndexController(Person person,Lion lion) {
        this.person = person;
        this.lion = lion;
    }

    public Person getPerson() {
        return person;
    }

    public Lion getLion() {
        return lion;
    }
}
