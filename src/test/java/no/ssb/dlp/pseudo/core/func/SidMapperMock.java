package no.ssb.dlp.pseudo.core.func;

import com.google.auto.service.AutoService;
import no.ssb.dapla.dlp.pseudo.func.map.Mapper;

import java.util.Map;

@AutoService(Mapper.class)
public class SidMapperMock implements Mapper {

    @Override
    public void init(Object data) {

    }

    @Override
    public void setConfig(Map<String, Object> config) {

    }

    @Override
    public Object map(Object data) {
        return null;
    }

    @Override
    public Object restore(Object mapped) {
        return null;
    }
}
