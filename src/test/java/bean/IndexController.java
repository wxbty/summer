package bean;

public class IndexController {

    private Person person;

    public IndexController(Person person) {
        this.person = person;
    }

    public Person getPerson() {
        return person;
    }
}
