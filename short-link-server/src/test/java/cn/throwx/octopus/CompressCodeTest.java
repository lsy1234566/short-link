package cn.throwx.octopus;

import cn.throwx.octopus.server.infra.util.ConversionUtils;
import org.junit.jupiter.api.Test;

public class CompressCodeTest {

    @Test
    public void generate() {
        Double pow = Math.pow(ConversionUtils.getSCALE(), 5);
        System.out.println(pow);
        String s = ConversionUtils.X.encode62(pow.longValue());
        System.out.println(s);
    }


    @Test
    public void generate2() {

        System.out.println(ConversionUtils.X.encode62(917336049));
        System.out.println(ConversionUtils.X.encode62(917336037));
        System.out.println(ConversionUtils.X.decode62("NoYxot"));

    }
}

