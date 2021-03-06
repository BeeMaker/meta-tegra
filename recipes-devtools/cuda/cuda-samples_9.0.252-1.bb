DESCRIPTION = "CUDA sample programs"
SRC_URI = ""
LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://EULA.txt;md5=1d9340fbe1f77282520c3ef05235c26a"

COMPATIBLE_MACHINE = "(tegra186|tegra210)"

do_fetch[noexec] = "1"
do_unpack[depends] += "${MLPREFIX}cuda-binaries:do_preconfigure dpkg-native:do_populate_sysroot"

PR = "r0"

unpack_samples() {
    dpkg-deb --fsys-tarfile ${TMPDIR}/work-shared/cuda-binaries-${PV}-${PR}/var/cuda-repo-9-0-local/cuda-samples-9-0_${PV}_arm64.deb | \
        tar --strip-components=5 --exclude="*/doc/*" --exclude="*/bin/*" -x -f- -C ${S}
}

python do_unpack() {
    bb.build.exec_func('unpack_samples', d)
}

inherit cuda

CUDA_NVCC_ARCH_FLAGS ??= ""

def extract_sm(d):
    archflags = d.getVar('CUDA_NVCC_ARCH_FLAGS').split()
    for flag in archflags:
        parts = flag.split('=')
        if len(parts) == 2 and parts[0] == '--gpu-code':
            return parts[1].split('_')[1]
    return ''

CUDA_SAMPLES ?= " \
    0_Simple/UnifiedMemoryStreams \
    1_Utilities/deviceQuery \
"

S = "${WORKDIR}/${BP}"
B = "${S}"

CUDA_PATH = "/usr/local/cuda-${CUDA_VERSION}"
CC_FIRST = "${@d.getVar('CC').split()[0]}"
CC_REST = "${@' '.join(d.getVar('CC').split()[1:])}"
CFLAGS += "-I=${CUDA_PATH}/include"
EXTRA_NVCCFLAGS = "-I${STAGING_DIR_HOST}${CUDA_PATH}/include"
LINKFLAGS = "-L${STAGING_DIR_HOST}${CUDA_PATH}/lib ${TOOLCHAIN_OPTIONS} ${@' '.join([f[4:] if f.startswith('-Wl,') else f for f in d.getVar('LDFLAGS').split()])} -lstdc++"

EXTRA_OEMAKE = ' \
    GENCODE_FLAGS="${CUDA_NVCC_ARCH_FLAGS}" SMS="${@extract_sm(d)}" OPENMP=yes \
    CUDA_PATH="${STAGING_DIR_NATIVE}/${CUDA_PATH}" HOST_COMPILER="${CC_FIRST}" CCFLAGS="${CC_REST} ${CFLAGS}" LDFLAGS="${LINKFLAGS}" \
    TARGET_ARCH="${TARGET_ARCH}" TARGET_OS="${TARGET_OS}" HOST_ARCH="${BUILD_ARCH}" \
'

do_compile() {
    oldwd="$PWD"
    for subdir in ${CUDA_SAMPLES}; do
        cd "$subdir"
        oe_runmake clean
        cd "$oldwd"
    done
}

do_compile() {
    oldwd="$PWD"
    for subdir in ${CUDA_SAMPLES}; do
        cd "$subdir"
        oe_runmake
        cd "$oldwd"
    done
}

do_install() {
    install -d ${D}${bindir}/cuda-samples
    for f in ${B}/bin/${TARGET_ARCH}/${TARGET_OS}/release/*; do
        [ -e "$f" ] || continue
        install -m 0755 "$f" ${D}${bindir}/cuda-samples/
    done
    dpkg-deb --fsys-tarfile ${TMPDIR}/work-shared/cuda-binaries-${PV}-${PR}/var/cuda-repo-9-0-local/cuda-samples-9-0_${PV}_arm64.deb | \
        tar --exclude="*usr/share*" -x -f- -C ${D}
}

INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
FILES_${PN} = "${bindir}/cuda-samples"
FILES_${PN}-dev = "${CUDA_PATH}"
INSANE_SKIP_${PN}-dev = "staticdev"
PACKAGE_ARCH = "${SOC_FAMILY_PKGARCH}"
