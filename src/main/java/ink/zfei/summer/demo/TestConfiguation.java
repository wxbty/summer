package ink.zfei.summer.demo;


import ink.zfei.summer.annation.Import;
import ink.zfei.summer.core.annation.Bean;
import ink.zfei.summer.core.annation.Configuration;

@Configuration
@Import(MyImportSelect.class)
public class TestConfiguation {


    @Bean
    public Person getPerson() {
        return new Person();
    }

    @Bean
    public Device getDevice() {
        Person person = getPerson();
        return new Device();
    }


}
