package no.uis.getlocalization;
import java.io.File;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;


public class GetTranslationsTest extends AbstractMojoTestCase {

  @Before
  protected void setUp() throws Exception {
    super.setUp();
  }

  @After
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testGetTranslations() throws Exception {
    
    File pom = getTestFile("src/test/resources", "get-translation-pom.xml");
    Assume.assumeNotNull(pom);
    Assume.assumeTrue(pom.canRead());
    
    GetTranslationMojo mojo = (GetTranslationMojo)lookupMojo("get-translation", pom);
    assertThat(mojo, is(notNullValue(GetTranslationMojo.class)));
    
    mojo.execute();
  }
}
