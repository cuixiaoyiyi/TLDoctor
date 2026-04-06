package Threadlocal.Example2;

class DoubleFormat {}
class PersonKryoSerializer {}

public class DummyTest {

    
    
    
    
    private static final ThreadLocal<DoubleFormat> DOUBLE_FORMAT = new ThreadLocal<>();

    
    
    
    
    public static ThreadLocal<PersonKryoSerializer> serializerCache = ThreadLocal.withInitial(PersonKryoSerializer::new);

    
    
    
    
    private final ThreadLocal<String> requestContext = new ThreadLocal<>();

    public void processTogglzState() {
        

        
        
        
        
        requestContext.set(null);
    }

    public void processSafeState() {
        

        
        requestContext.remove();
    }
}