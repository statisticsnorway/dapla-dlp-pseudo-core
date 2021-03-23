package no.ssb.dlp.pseudo.core.util;

import org.junit.jupiter.api.Test;

import static no.ssb.dlp.pseudo.core.util.PathJoiner.joinAndKeepLeadingAndTrailingSlash;
import static no.ssb.dlp.pseudo.core.util.PathJoiner.joinAndKeepLeadingSlash;
import static no.ssb.dlp.pseudo.core.util.PathJoiner.joinAndKeepTrailingSlash;
import static no.ssb.dlp.pseudo.core.util.PathJoiner.joinWithoutLeadingOrTrailingSlash;

import static org.assertj.core.api.Assertions.assertThat;

class PathJoinerTest {

    @Test
    void testJoinWithoutLeadingOrTrailingSlash() {
        assertThat(joinWithoutLeadingOrTrailingSlash("/first/", "/second/third/")).isEqualTo("first/second/third");
        assertThat(joinWithoutLeadingOrTrailingSlash("first", "second/third", "fourth")).isEqualTo("first/second/third/fourth");
        assertThat(joinWithoutLeadingOrTrailingSlash("first", null, "second")).isEqualTo("first/second");
        assertThat(joinWithoutLeadingOrTrailingSlash("first", null, "", " ", "second")).isEqualTo("first/second");
    }

    @Test
    void testJoinAndKeepLeadingSlash() {
        assertThat(joinAndKeepLeadingSlash("/first/", "/second/third/")).isEqualTo("/first/second/third");
        assertThat(joinAndKeepLeadingSlash("first", "second/third", "fourth")).isEqualTo("/first/second/third/fourth");
        assertThat(joinAndKeepLeadingSlash("first", null, "second")).isEqualTo("/first/second");
        assertThat(joinAndKeepLeadingSlash("first", null, "", " ", "second")).isEqualTo("/first/second");
    }

    @Test
    void testJoinAndKeepTrailingSlash() {
        assertThat(joinAndKeepTrailingSlash("/first/", "/second/third/")).isEqualTo("first/second/third/");
        assertThat(joinAndKeepTrailingSlash("/first", "/second/third", "fourth")).isEqualTo("first/second/third/fourth/");
        assertThat(joinAndKeepTrailingSlash("/first", null, "second")).isEqualTo("first/second/");
        assertThat(joinAndKeepTrailingSlash("/first", null, "", " ", "second")).isEqualTo("first/second/");
    }

    @Test
    void testJoinAndKeepLeadingAndTrailingSlash() {
        assertThat(joinAndKeepLeadingAndTrailingSlash("/first/", "/second/third/")).isEqualTo("/first/second/third/");
        assertThat(joinAndKeepLeadingAndTrailingSlash("first", "second/third", "fourth")).isEqualTo("/first/second/third/fourth/");
        assertThat(joinAndKeepLeadingAndTrailingSlash("first", null, "second")).isEqualTo("/first/second/");
        assertThat(joinAndKeepLeadingAndTrailingSlash("first", null, "", " ", "second")).isEqualTo("/first/second/");
    }
}