package pt.unl.fct.scc.sccbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebSecurity
class SccBackendSecurityConfiguration : WebSecurityConfigurerAdapter() {

	override fun configure(http: HttpSecurity) {
		http.authorizeRequests { it.anyRequest().permitAll() }
	}

}

@Configuration
@EnableWebMvc
class SccBackendConfiguration : WebMvcConfigurer

@SpringBootApplication
class SccBackendApplication

fun main(args: Array<String>) {
	runApplication<SccBackendApplication>(*args)
}
