package org.springframework.xd.greenplum.support;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by cq on 4/4/16.
 */
public class LoadReadableTableFactoryBeanTest {

    @Test
    public void testLoadHeaderConfiguration() throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "org/springframework/xd/greenplum/support/LoadReadableTableFactoryBeanTest.xml");
        ReadableTableFactoryBean factoryBean = context.getBean("&greenplumReadableTable", ReadableTableFactoryBean.class);
        assertThat(factoryBean.isHeader(),is(true));
        context.close();

    }
}
