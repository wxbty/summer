package context.annotation;


import bean.Device;
import bean.Person;
import bean.Player;
import bean.configuration.MyImportSelect;
import ink.zfei.summer.annation.Import;
import ink.zfei.summer.core.annation.Bean;
import ink.zfei.summer.core.annation.Configuration;

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
        return new Device();
    }


}
