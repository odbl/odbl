FROM openjdk:11-jre

ENV VERTICLE_FILE odbl-fat.jar
# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles
# Set the location of the logback file
ENV LOG_HOME $VERTICLE_HOME/logs
ENV LOG_FILENAME odbl.log
# Set the log level
ENV LOG_LEVEL_FILE INFO

EXPOSE 8080

RUN addgroup --system vertx && adduser --system --group vertx

# Copy your fat jar to the container
COPY target/$VERTICLE_FILE $VERTICLE_HOME/
COPY conf/config.json $VERTICLE_HOME/conf/

RUN chown -R vertx $VERTICLE_HOME
RUN chmod -R g+w $VERTICLE_HOME

# Create log folder
RUN mkdir $LOG_HOME

RUN chown -R vertx $LOG_HOME
RUN chmod -R g+w $LOG_HOME

USER vertx

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java $JAVA_OPTS -jar $VERTICLE_FILE"]
