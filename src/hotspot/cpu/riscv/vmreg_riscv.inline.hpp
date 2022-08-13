/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2022, Huawei Technologies Co., Ltd. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#ifndef CPU_RISCV_VM_VMREG_RISCV_INLINE_HPP
#define CPU_RISCV_VM_VMREG_RISCV_INLINE_HPP

inline VMReg Register::RegisterImpl::as_VMReg() const {
  return VMRegImpl::as_VMReg(encoding() * Register::max_slots_per_register);
}

inline VMReg FloatRegister::FloatRegister::FloatRegisterImpl::as_VMReg() const {
  return VMRegImpl::as_VMReg((encoding() * FloatRegister::max_slots_per_register) +
                             ConcreteRegisterImpl::max_gpr);
}

inline VMReg VectorRegister::VectorRegisterImpl::as_VMReg() const {
  return VMRegImpl::as_VMReg((encoding() * VectorRegister::max_slots_per_register) +
                             ConcreteRegisterImpl::max_fpr);
}

#endif // CPU_RISCV_VM_VMREG_RISCV_INLINE_HPP
