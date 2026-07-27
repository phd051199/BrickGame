package brickgame;

final class CpuFactory {
    private CpuFactory() {
    }

    static BrickCpu create(MachineProfile profile, byte[] rom) {
        if (profile.coreType == MachineProfile.CORE_HT943) {
            return new Ht943Cpu(profile, rom);
        }
        if (profile.coreType == MachineProfile.CORE_SPL02
                || profile.coreType == MachineProfile.CORE_SPL03) {
            return new Spl0XCpu(profile, rom);
        }
        if (profile.coreType == MachineProfile.CORE_EM73000) {
            return new Em73000Cpu(profile, rom);
        }
        if (profile.coreType == MachineProfile.CORE_E0C6200) {
            return new E0C6200Cpu(profile, rom);
        }
        if (profile.coreType == MachineProfile.CORE_KS56) {
            return new Ks56Cpu(profile, rom);
        }
        throw new IllegalArgumentException("Unsupported Brick CPU");
    }
}
