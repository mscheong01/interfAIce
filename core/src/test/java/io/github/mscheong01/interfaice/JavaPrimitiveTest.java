package io.github.mscheong01.interfaice;

import org.junit.jupiter.api.Test;

import io.github.mscheong01.interfaice.openai.OpenAiProxyFactory;

public class JavaPrimitiveTest {
    TextObjectTranscoder transcoder = new TextObjectTranscoder();

    interface TestInterface {
        int sum(int a, int b);
    }

    TestInterface proxy = OpenAiProxyFactory.of(System.getenv("OPENAI_API_KEY"))
            .create(TestInterface.class);

    @Test
    public void test() {
        int result = proxy.sum(1, 2);
        System.out.println(result);
    }
}
