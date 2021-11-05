package pt.unl.fct.scc.sccbackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import pt.unl.fct.scc.sccbackend.common.accessControl.UserResolver
import pt.unl.fct.scc.sccbackend.common.pagination.PaginationResolver

@Configuration
@EnableWebMvc
class SccBackendConfiguration : WebMvcConfigurer {

	override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
		converters.clear()
		converters.add(0, KotlinSerializationJsonHttpMessageConverter(Json {
			prettyPrint = true
		}))

		// Jackson as fallback
		val mapper = ObjectMapper().registerKotlinModule()
		converters.add(1, MappingJackson2HttpMessageConverter(mapper))
	}

	@Bean
	fun getPasswordEncoder(): PasswordEncoder {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder()
	}

	@Autowired
	private lateinit var userResolver: UserResolver

	@Autowired
	private lateinit var paginationResolver: PaginationResolver

	override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
		resolvers.add(userResolver)
		resolvers.add(paginationResolver)
	}

}

@SpringBootApplication(exclude = [MongoAutoConfiguration::class, MongoReactiveAutoConfiguration::class])
class SccBackendApplication

fun main(args: Array<String>) {
	runApplication<SccBackendApplication>(*args)
}
