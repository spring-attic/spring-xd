package org.springframework.xd.dirt.server;

/**
 * The kind of transport used for communication between the admin server and container(s).
 *  
 * @author Eric Bottard
 */
public enum Transport {

	/**
	 * Use same-process communication, using an in memory queue.
	 */
	local, 
	
	/**
	 * Use redis (http://redis.io) as the communication middleware.
	 */
	redis,

//	/**
//	 * Use RabbitMQ (http://www.rabbitmq.com/) as the communication middleware.
//	 */
//	rabbitmq
	; 
	
}
