package bg.petarh.piano.api.contract;

import bg.petarh.piano.api.rpc.ApiEvent;
import bg.petarh.piano.api.rpc.MayBeAbsent;
import bg.petarh.piano.api.rpc.MethodSignature;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TypeScriptContractTest {

    record Note(String pitch, int midi) {
    }

    record Chord(String start, @MayBeAbsent String name, List<Note> notes, Map<String, Integer> extra) {
    }

    record Request(String key, Integer tempo, boolean flag) {
    }

    record Unsupported(Object anything) {
    }

    @Test
    void writesInterfacesAndTheMethodAndEventMaps() {
        String contract = TypeScriptContract.generate(
                List.of(new MethodSignature("chord.next", Request.class, Chord.class),
                        new MethodSignature("chord.stop", Void.class, Void.class)),
                List.of(ApiEvent.of("note", Note.class)));

        assertThat(contract).endsWith("""

                export interface Chord {
                  start: string;
                  name?: string;
                  notes: Note[];
                  extra: Record<string, number>;
                }

                export interface Note {
                  pitch: string;
                  midi: number;
                }

                export interface Request {
                  key?: string;
                  tempo?: number;
                  flag?: boolean;
                }

                export interface ApiMethods {
                  "chord.next": { params: Request; result: Chord };
                  "chord.stop": { params: Record<string, never>; result: null };
                }

                export interface ApiEvents {
                  "note": Note;
                }
                """);
        assertThat(contract).startsWith("// Generated from the Java API records");
    }

    @Test
    void refusesTypesItCannotDescribe() {
        assertThatIllegalArgumentException().isThrownBy(() -> TypeScriptContract.generate(
                List.of(new MethodSignature("x", Unsupported.class, Void.class)), List.of()));
    }
}
