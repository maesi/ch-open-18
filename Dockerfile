FROM jboss/wildfly:14.0.0.Final

ADD target/italians.war /opt/jboss/wildfly/standalone/deployments/