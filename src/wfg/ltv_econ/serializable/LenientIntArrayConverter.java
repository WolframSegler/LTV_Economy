package wfg.ltv_econ.serializable;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class LenientIntArrayConverter implements Converter {
    @Override
    @SuppressWarnings("rawtypes")
    public final boolean canConvert(Class type) {
        return type == int[].class;
    }

    @Override
    public final void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        final int[] array = (int[]) source;
        if (array.length == 0) {
            writer.setValue("");
        } else {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length; i++) {
                if (i > 0) sb.append('|');
                sb.append(array[i]);
            }
            writer.setValue(sb.toString());
        }
    }

    @Override
    public final Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        final String data = reader.getValue();
        if (data == null || data.trim().isEmpty()) {
            return new int[0];
        }
        final String[] parts = data.split("\\|");
        final int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i]);
        }
        return result;
    }
}