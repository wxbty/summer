package context.annotation;


import bean.Area;
import bean.Device;
import bean.Person;
import bean.Player;
import bean.configuration.MyImportSelect;
import ink.zfei.summer.annation.Import;
import ink.zfei.summer.core.annotation.Bean;
import ink.zfei.summer.core.annotation.Configuration;

@Configuration
@Import({Player.class, MyImportSelect.class})
public class MyConfiguation {


    @Bean
    public Person person() {
        return new Person();
    }

    @Bean
    public Device device() {
        Device device = new Device();
        device.setPerson(person());
        return device;
    }

    @Bean
    public Player player() {
        return new Player();
    }

    @Bean
    public Area area() {
        return new Area();
    }


}
