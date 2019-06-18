package recommendation;

import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import recommendation.common.HibernateUtil;
import recommendation.hibernate.PersistentCategories;
import recommendation.hibernate.PersistentHistory;
import recommendation.hibernate.PersistentItem;
import recommendation.hibernate.PersistentUser;

@Configuration
@EnableWebMvc
public class ApplicationConfiguration {

	@Bean(name="restTemplate")
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}

	@Bean(name = "sessionFactory")
    public SessionFactory getSessionFactory() {
               return HibernateUtil.createSessionFactory(
                          PersistentItem.class,
                          PersistentCategories.class,
                          PersistentHistory.class,
                          PersistentUser.class);
    }

}

