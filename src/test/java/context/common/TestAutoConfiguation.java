package context.common;


import bean.Water;
import ink.zfei.summer.core.annotation.Bean;
import ink.zfei.summer.core.annotation.Configuration;

@Configuration
public class TestAutoConfiguation {


    @Bean
    public Water water() {
        return new Water();
    }


}
