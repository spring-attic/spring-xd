import org.springframework.xd.extensions.test.StubPojoToStringConverter

beans {
	xmlns util:"http://www.springframework.org/schema/util"
	util.list(id:'customMessageConverters') {
		bean('class':StubPojoToStringConverter.class.name)
	}
}
