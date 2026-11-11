package id.ac.sekolah.manajemenkelas.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GenderMapperTest {

    @Test
    void convertsJavaGenderToPostgresAndBack() {
        assertEquals("LAKI_LAKI", GenderMapper.toDatabaseGender("L"));
        assertEquals("LAKI_LAKI", GenderMapper.toDatabaseGender("l"));
        assertEquals("PEREMPUAN", GenderMapper.toDatabaseGender("P"));
        assertEquals("PEREMPUAN", GenderMapper.toDatabaseGender("p"));

        assertEquals("L", GenderMapper.fromDatabaseGender("LAKI_LAKI"));
        assertEquals("P", GenderMapper.fromDatabaseGender("PEREMPUAN"));
    }

    @Test
    void rejectsInvalidOrBlankGender() {
        assertThrows(IllegalArgumentException.class, () -> GenderMapper.toDatabaseGender(null));
        assertThrows(IllegalArgumentException.class, () -> GenderMapper.toDatabaseGender(""));
        assertThrows(IllegalArgumentException.class, () -> GenderMapper.toDatabaseGender("X"));
        assertThrows(IllegalArgumentException.class, () -> GenderMapper.fromDatabaseGender("UNKNOWN"));
    }
}

