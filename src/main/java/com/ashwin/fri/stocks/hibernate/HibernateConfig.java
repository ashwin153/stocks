package com.ashwin.fri.stocks.hibernate;

import java.io.IOException;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import com.ashwin.fri.stocks.model.Number;
import com.ashwin.fri.stocks.model.Registrant;
import com.ashwin.fri.stocks.model.Submission;
import com.ashwin.fri.stocks.model.Tag;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class HibernateConfig {
	
	private static final String HIBERNATE_PROPERTIES = "/hibernate.properties";

	/**
	 * Creates a hibernate session factory from the properties file defined in
	 * src/main/resources/hibernate.properties.
	 * 
	 * @return hibernate session
	 * @throws IOException
	 */
	public static SessionFactory getHibernateSessionFactory() throws IOException {
		// Step 1: Load the properties file
		Properties props = new Properties();
		props.load(HibernateConfig.class.getResourceAsStream(HIBERNATE_PROPERTIES));
				
		// Step 2: Configure the connection pool to the database
		HikariConfig hikari = new HikariConfig();
		hikari.setMaximumPoolSize(1);
		hikari.setDataSourceClassName(props.getProperty("dataSource.className"));
		hikari.addDataSourceProperty("serverName", props.getProperty("dataSource.serverName"));
		hikari.addDataSourceProperty("port", props.getProperty("dataSource.port"));
		hikari.addDataSourceProperty("databaseName", props.getProperty("dataSource.databaseName"));
		hikari.addDataSourceProperty("user", props.getProperty("dataSource.user"));
		hikari.addDataSourceProperty("password", props.getProperty("dataSource.password"));
		HikariDataSource hds = new HikariDataSource(hikari);
		
		// Step 3: Configure the Hibernate connection to the database
		Configuration config = new Configuration()
			.addAnnotatedClass(Tag.class)
			.addAnnotatedClass(Number.class)
			.addAnnotatedClass(Registrant.class)
			.addAnnotatedClass(Submission.class)
			.addProperties(props);
		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
			.applySettings(config.getProperties())
			.applySetting(Environment.DATASOURCE, hds);
		return config.buildSessionFactory(builder.build());
	}
	
}
