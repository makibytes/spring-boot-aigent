package de.makibytes.aigent.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Aigent starter.
 *
 * <pre>
 * aigent.on-stub=WARN          # WARN (default) | FAIL | IGNORE
 * aigent.contracts=ENFORCE     # ENFORCE (default) | MONITOR | OFF
 * aigent.pure-check=true       # true (default) | false
 * </pre>
 */
@ConfigurationProperties(prefix = "aigent")
public class AigentProperties {

    private OnStub onStub = OnStub.WARN;
    private ContractMode contracts = ContractMode.ENFORCE;
    private boolean pureCheck = true;

    public OnStub getOnStub() { return onStub; }
    public void setOnStub(OnStub onStub) { this.onStub = onStub; }

    public ContractMode getContracts() { return contracts; }
    public void setContracts(ContractMode contracts) { this.contracts = contracts; }

    public boolean isPureCheck() { return pureCheck; }
    public void setPureCheck(boolean pureCheck) { this.pureCheck = pureCheck; }

    public enum OnStub {
        WARN, FAIL, IGNORE
    }

    public enum ContractMode {
        /** Throw on violation. Default for development. */
        ENFORCE,
        /** Log violation as warning, continue execution. Good for staging/observation. */
        MONITOR,
        /** Skip all contract evaluation. Recommended for production if overhead is a concern. */
        OFF
    }
}
