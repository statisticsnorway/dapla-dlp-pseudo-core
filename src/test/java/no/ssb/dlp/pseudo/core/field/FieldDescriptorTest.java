package no.ssb.dlp.pseudo.core.field;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FieldDescriptorTest {

    static FieldDescriptor field(String f) {
        return new FieldDescriptor(f);
    } 

    @Test
    void newField_givenPathString_shouldDeduceFieldName() {
        assertThat(field("/path/to/a/prop")).hasFieldOrPropertyWithValue("name", "prop");
        assertThat(field("path/to/a/prop")).hasFieldOrPropertyWithValue("name", "prop");
        assertThat(field("prop")).hasFieldOrPropertyWithValue("name", "prop");
        assertThat(field("")).hasFieldOrPropertyWithValue("name", "");
        assertThat(field(null)).hasFieldOrPropertyWithValue("name", "");
    }

    @Test
    void newField_givenPathString_shouldNormalizePath() {
        assertThat(field("/path/to/a/prop")).hasFieldOrPropertyWithValue("path", "/path/to/a/prop");
        assertThat(field("path/to/a/prop")).hasFieldOrPropertyWithValue("path", "/path/to/a/prop");
        assertThat(field("prop")).hasFieldOrPropertyWithValue("path", "/prop");
        assertThat(field("")).hasFieldOrPropertyWithValue("path", "/");
        assertThat(field(null)).hasFieldOrPropertyWithValue("path", "/");
    }

    @Test
    void field_givenGlobExpression_shouldMatch() {
        assertThat(field("/path/to/a/prop").globMatches("/path/to/a/prop")).isTrue();
        assertThat(field("/path/to/a/prop").globMatches("/path/{to,from}/a/prop")).isTrue();
        assertThat(field("/path/to/a/prop").globMatches("/path/**/a/prop")).isTrue();
        assertThat(field("/path/to/a/prop").globMatches("/path/**/prop")).isTrue();
        assertThat(field("/path/to/a/prop").globMatches("/path/**/[a-z]*")).isTrue();
        assertThat(field("/path/to/a/prop").globMatches("/path/to/a/p?*")).isTrue();
        assertThat(field("/path/to/a/prop").globMatches("**/prop")).isTrue();
        assertThat(field("/path/to/a/prop").globMatches("**/{p,q}rop")).isTrue();
        assertThat(field("/path/to/a/prop").globMatches("**/{t*,from}/a/prop")).isTrue();
        assertThat(field("/path/to/a/prop").globMatches("**/prop")).isTrue();
        assertThat(field("/path/to/a/prop").globMatches("**/to/*/pro?")).isTrue();
        assertThat(field("/some/path/with/?and*").globMatches("**/\\?and\\*")).isTrue();
        assertThat(field("prop").globMatches("/prop")).isTrue();
        assertThat(field("").globMatches("/")).isTrue();
    }

    @Test
    void field_givenGlobExpression_shouldNotMatch() {
        assertThat(field("/path/to/a/prop").globMatches("*/prop")).isFalse();
        assertThat(field("/path/to/a/prop").globMatches("")).isFalse();
        assertThat(field("/path/to/a/prop").globMatches(null)).isFalse();
        assertThat(field("/path/to/a/prop").globMatches("/path/**/some/prop")).isFalse();
        assertThat(field("/path/to/a/prop").globMatches("/path/**/propp")).isFalse();
        assertThat(field("prop").globMatches("prop")).isFalse();
        assertThat(field("null").globMatches(null)).isFalse();
        assertThat(field("").globMatches("")).isFalse();
    }

}