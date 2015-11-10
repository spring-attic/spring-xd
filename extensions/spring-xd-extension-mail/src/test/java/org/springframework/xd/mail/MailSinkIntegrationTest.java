/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.mail;

import com.icegreen.greenmail.util.*;
import org.junit.*;
import org.springframework.xd.dirt.server.singlenode.SingleNodeApplication;
import org.springframework.xd.dirt.test.SingleNodeIntegrationTestSupport;
import org.springframework.xd.dirt.test.SingletonModuleRegistry;
import org.springframework.xd.dirt.test.process.SingleNodeProcessingChainProducer;
import org.springframework.xd.dirt.test.process.SingleNodeProcessingChainSupport;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.test.RandomConfigurationSupport;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.Security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Franck MARCHAND
 */
public class MailSinkIntegrationTest {
    public static final int              TIMEOUT = 5000;
    private static SingleNodeApplication application;

    private static GreenMail greenMail = new GreenMail(new ServerSetup[] { ServerSetupTest.SMTPS, ServerSetupTest.SMTP, ServerSetupTest.IMAPS });


    @BeforeClass
    public static void setUp() {
        Security.setProperty("ssl.SocketFactory.provider", DummySSLSocketFactory.class.getName());
        greenMail.setUser("someone@somewhere.fr", "someone@somewhere.fr", "test1");
        greenMail.setUser("nobody@nowhere.fr", "nobody@nowhere.fr", "test1");
        greenMail.start();

        new RandomConfigurationSupport();
        application = new SingleNodeApplication().run();
        SingleNodeIntegrationTestSupport singleNodeIntegrationTestSupport = new SingleNodeIntegrationTestSupport(application);
        singleNodeIntegrationTestSupport.addModuleRegistry(new SingletonModuleRegistry(ModuleType.sink, "mail"));
    }

    @Test
    public void testSSLMailSinkWithAttachmentsIntegration() throws IOException, InterruptedException, MessagingException {

        String filePath = Paths.get("src/test/resources/attachment.txt").toAbsolutePath().toString();
        String filePath2 = Paths.get("src/test/resources/attachment2.txt").toAbsolutePath().toString();
        String filePath3 = Paths.get("src/test/resources/attachment3.txt").toAbsolutePath().toString();

        SingleNodeProcessingChainProducer chain = SingleNodeProcessingChainSupport.chainProducer(application, "testMailSink", String.format(
                        "mail --host=localhost " + "--to='''nobody@nowhere.fr''' --from='''someone@somewhere.fr''' --replyTo='''nobody@nowhere.fr''' "
                                + "--subject='''testXD''' --port=3465 --username='someone@somewhere.fr' --password='test1' " + "--ssl=true --auth=true --attachmentExpression='''%s;%s;%s''' "
                        + "--attachmentFilename='''test.txt;test2.txt;test3.txt'''", filePath, filePath2, filePath3));

        chain.sendPayload(filePath);

        assertThat(greenMail.waitForIncomingEmail(5000, 1), is(true));
        chain.destroy();

        Message[] messages = greenMail.getReceivedMessages();
        assertThat(messages.length, is(1));

        assertThat(messages[0].getSubject(), is("testXD"));

        MimeMultipart mp = (MimeMultipart)messages[0].getContent();

        assertThat(mp.getCount(), is(4));
        boolean allPartsArePresents = true;

        for(int i=0;i<4;i++) {
            try {
                if(mp.getBodyPart(i) == null)
                    allPartsArePresents = false;
            } catch (MessagingException e) {
                Assert.fail();
            }
        }

        assertThat(allPartsArePresents, is(true));
    }

    @Test
    public void testUnsecuredMailSinkIntegration() throws IOException, InterruptedException, MessagingException {
        greenMail.reset();

        String filePath = Paths.get("src/test/resources/attachment.txt").toAbsolutePath().toString();
        String filePath2 = Paths.get("src/test/resources/attachment2.txt").toAbsolutePath().toString();
        String filePath3 = Paths.get("src/test/resources/attachment3.txt").toAbsolutePath().toString();

        SingleNodeProcessingChainProducer chain = SingleNodeProcessingChainSupport.chainProducer(application, "testMailSink2", String.format(
                "mail --host=localhost " + "--to='''nobody@nowhere.fr''' --from='''nobody@nowhere.fr''' --replyTo='''nobody@nowhere.fr''' "
                        + "--subject='''testXD2''' --port=3025 --username='someone@somewhere.fr' --password='test1' "
                        + "--starttls=false --ssl=false --auth=true --attachmentExpression='''%s;%s;%s''' "
                        + "--attachmentFilename='''test.txt;test2.txt;test3.txt'''",
                filePath, filePath2,filePath3));

        chain.sendPayload(filePath);

        assertThat(greenMail.waitForIncomingEmail(TIMEOUT, 1), is(true));
        chain.destroy();

        Message[] messages = greenMail.getReceivedMessages();
        assertThat(messages.length, is(1));

        assertThat(messages[0].getSubject(), is("testXD2"));

        MimeMultipart mp = (MimeMultipart)messages[0].getContent();

        assertThat(mp.getCount(), is(4));

        boolean allPartsArePresents = true;

        for(int i=0;i<4;i++) {
            try {
                if(mp.getBodyPart(i) == null)
                    allPartsArePresents = false;
            } catch (MessagingException e) {
                Assert.fail();
            }
        }

        assertThat(allPartsArePresents, is(true));

    }

    @AfterClass
    public static void tearDown() {
        greenMail.stop();
    }
}
