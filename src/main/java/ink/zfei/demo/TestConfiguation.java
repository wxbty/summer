package ink.zfei.demo;


import ink.zfei.core.annation.Bean;
import ink.zfei.core.annation.Configuration;

@Configuration
public class TestConfiguation {

    @Bean
    public Person getPerson() {
        return new Person();
    }

}
