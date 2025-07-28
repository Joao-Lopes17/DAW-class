package pt.isel
import kotlinx.datetime.Clock
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import pt.isel.mem.TransactionManagerInMem
import kotlin.time.Duration.Companion.hours

@Configuration
class PipelineConfigurer(
    val authenticationInterceptor: AuthenticationInterceptor,
    val authenticatedUserArgumentResolver: AuthenticatedUserArgumentResolver,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
       registry.addInterceptor(authenticationInterceptor)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authenticatedUserArgumentResolver)
    }
}

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:8000", "http://localhost:4040") // Replace with your frontend URL
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
    }
}

@SpringBootApplication
class AppIMSystem {
    @Bean
    fun jdbi() =
        Jdbi
            .create(
                PGSimpleDataSource().apply {
                    setURL(Environment.getDbUrl())
                    user = "postgres"
                    password = "changeit"
                },
            ).configureWithAppRequirements()

    @Bean
    @Profile("jdbi")
    fun trxManagerJdbi(jdbi: Jdbi): TransactionManagerJdbi = TransactionManagerJdbi(jdbi)



   @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun tokenEncoder() = Sha256TokenEncoder()

    @Bean
    fun clock() = Clock.System

   @Bean
    fun usersDomainConfig() =
        UsersDomainConfig(
            tokenSizeInBytes = 256 / 8,
            tokenTtl = 24.hours,
            tokenRollingTtl = 1.hours,
            maxTokensPerUser = 3,
        )
}

fun main() {
    runApplication<AppIMSystem>()
}