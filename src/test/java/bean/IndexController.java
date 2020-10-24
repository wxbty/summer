package bean;

public class IndexController {

    private Person person;
    private Lion lion;

    public IndexController(Person person,Lion lion) {
        this.person = person;
    }

    public Person getPerson() {
        return person;
    }

    public Lion getLion() {
        return lion;
    }
}
