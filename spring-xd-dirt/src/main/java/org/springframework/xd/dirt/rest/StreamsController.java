package org.springframework.xd.dirt.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.xd.dirt.stream.StreamDeployer;

@Controller
@RequestMapping("/streams")
public class StreamsController {

	@Autowired
	private StreamDeployer streamDeployer;
	
	@RequestMapping(value="/{name}", method= {RequestMethod.PUT, RequestMethod.POST})
	public void deploy(@PathVariable("name") String name, @RequestBody String dsl) {
		streamDeployer.deployStream(name, dsl);
	}
	
	@RequestMapping(value="/{name}", method = RequestMethod.DELETE)
	public void deploy(@PathVariable("name") String name) {
		streamDeployer.undeployStream(name);
	}
	
	
}
