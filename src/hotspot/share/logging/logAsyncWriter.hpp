/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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
#ifndef SHARE_LOGGING_LOGASYNCWRITER_HPP
#define SHARE_LOGGING_LOGASYNCWRITER_HPP
#include "logging/log.hpp"
#include "logging/logDecorations.hpp"
#include "logging/logMessageBuffer.hpp"
#include "memory/allocation.hpp"
#include "runtime/mutex.hpp"
#include "runtime/nonJavaThread.hpp"
#include "runtime/semaphore.hpp"
#include "utilities/resourceHash.hpp"

// Forward declaration
class LogFileStreamOutput;

class AsyncLogMessage {
  LogFileStreamOutput* _output;
  const char* _message;
  const LogDecorations _decorations;

public:
  AsyncLogMessage(LogFileStreamOutput* output, const LogDecorations& decorations, const char* msg)
    : _output(output), _message(msg), _decorations(decorations) {}

  size_t size() const {
    constexpr size_t size = align_up(sizeof(*this), sizeof(void*));
    return _message != nullptr ? align_up(size + strlen(_message) + 1, sizeof(void*)): size;
  }

  LogFileStreamOutput* output() const { return _output; }
  const LogDecorations& decorations() const { return _decorations; }
  void set_message(const char* msg) { _message = msg; }
  const char* message() const { return _message; }
};

typedef ResourceHashtable<LogFileStreamOutput*,
                          uint32_t,
                          17, /*table_size*/
                          ResourceObj::C_HEAP,
                          mtLogging> AsyncLogMap;

//
// ASYNC LOGGING SUPPORT
//
// Summary:
// Async Logging is working on the basis of singleton AsyncLogWriter, which manages an intermediate buffer and a flushing thread.
//
// Interface:
//
// initialize() is called once when JVM is initialized. It creates and initializes the singleton instance of AsyncLogWriter.
// Once async logging is established, there's no way to turn it off.
//
// instance() is MT-safe and returns the pointer of the singleton instance if and only if async logging is enabled and has
// successfully initialized. Clients can use its return value to determine async logging is established or not.
//
// enqueue() is the basic operation of AsyncLogWriter. Two overloading versions of it are provided to match LogOutput::write().
// They are both MT-safe and non-blocking. Derived classes of LogOutput can invoke the corresponding enqueue() in write() and
// return 0. AsyncLogWriter is responsible of copying necessary data.
//
// flush() ensures that all pending messages have been written out before it returns. It is not MT-safe in itself. When users
// change the logging configuration via jcmd, LogConfiguration::configure_output() calls flush() under the protection of the
// ConfigurationLock. In addition flush() is called during JVM termination, via LogConfiguration::finalize.
class AsyncLogWriter : public NonJavaThread {
  class AsyncLogLocker;
  friend class AsyncLogTest;

  class Buffer : public CHeapObj<mtLogging> {
    size_t _pos;
    char* _buf;
    size_t _capacity;

   public:
    Buffer(size_t capacity);
    ~Buffer();

    bool is_valid() const;

    bool push_back(const AsyncLogMessage& msg);

    // for testing-only!
    size_t set_capacity(size_t value) {
      size_t old = _capacity;
      _capacity = value;
      return old;
    }

    void reset() { _pos = 0; }

    class Iterator {
      const Buffer& _buf;
      size_t _curr;

      void* raw_ptr() const {
        assert(_curr < _buf._pos, "sanity check");
        return _buf._buf + _curr;
      }

    public:
      Iterator(const Buffer& buffer): _buf(buffer), _curr(0) {}

      bool is_empty() const {
        return _curr >= _buf._pos;
      }

      AsyncLogMessage* next();
    };

    Iterator iterator() const {
      return Iterator(*this);
    }
  };

  static AsyncLogWriter* _instance;
  Semaphore _flush_sem;
  // Can't use a Monitor here as we need a low-level API that can be used without Thread::current().
  PlatformMonitor _lock;
  bool _data_available;
  volatile bool _initialized;
  AsyncLogMap _stats; // statistics for dropped messages

  // ping-pong buffers
  Buffer* _buffer;
  Buffer* _buffer_staging;

  static const LogDecorations& None;
  static const AsyncLogMessage& Token;

  AsyncLogWriter();
  void enqueue_locked(const AsyncLogMessage& msg);
  void write();
  void run() override;
  void pre_run() override {
    NonJavaThread::pre_run();
    log_debug(logging, thread)("starting AsyncLog Thread tid = " INTX_FORMAT, os::current_thread_id());
  }
  const char* name() const override { return "AsyncLog Thread"; }
  const char* type_name() const override { return "AsyncLogWriter"; }
  void print_on(outputStream* st) const override {
    st->print("\"%s\" ", name());
    Thread::print_on(st);
    st->cr();
  }

  // for testing-only
  size_t throttle_buffers(size_t newsize);
 public:
  void enqueue(LogFileStreamOutput& output, const LogDecorations& decorations, const char* msg);
  void enqueue(LogFileStreamOutput& output, LogMessageBuffer::Iterator msg_iterator);

  static AsyncLogWriter* instance();
  static void initialize();
  static void flush();

};

#endif // SHARE_LOGGING_LOGASYNCWRITER_HPP
