package pt.isel

import pt.isel.mem.TransactionManagerInMem
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component


@Component
class TestConfig {
    @Bean
    @Profile("inMem")
    fun trxManagerInMem(): TransactionManager = TransactionManagerInMem()
}
