/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1EVACFAILURE_HPP
#define SHARE_GC_G1_G1EVACFAILURE_HPP

#include "gc/g1/g1OopClosures.hpp"
#include "gc/g1/heapRegionManager.hpp"
#include "gc/shared/workerThread.hpp"
#include "utilities/globalDefinitions.hpp"

class G1CollectedHeap;
class G1EvacFailureRegions;

// Task to fixup self-forwarding pointers
// installed as a result of an evacuation failure.
class G1ParRemoveSelfForwardPtrsTask : public WorkerTask {
  G1CollectedHeap* _g1h;
  bool _during_concurrent_start;
  G1EvacFailureRegions* _evac_failure_regions;
  CHeapBitMap _chunk_bitmap;

  // Initialized outside of the constructor because #workers was unknown at that time
  uint _num_chunks_per_region;
  uint _num_evac_fail_regions;
  size_t _chunk_size;

  bool claim_chunk(uint chunk_idx) {
    return _chunk_bitmap.par_set_bit(chunk_idx);
  }

  class RegionMarkedWordsCache;
  void process_chunk(uint worker_id, uint chunk_idx, RegionMarkedWordsCache* cache);
public:
  explicit G1ParRemoveSelfForwardPtrsTask(G1EvacFailureRegions* evac_failure_regions);

  void work(uint worker_id);

  void pre_start(uint num_workers) {
    _num_evac_fail_regions = _evac_failure_regions->num_regions_failed_evacuation();
    // Same heuristic as G1RemSetScanState
    _num_chunks_per_region = 1u << (HeapRegion::LogOfHRGrainBytes / 2 - 4);
    assert(HeapRegion::GrainWords % _num_chunks_per_region == 0, "inv");
    _chunk_size = static_cast<uint>(HeapRegion::GrainWords / _num_chunks_per_region);
    log_debug(gc, ergo)("Initializing removing self forwards with %u chunks per region given %u workers",
                        _num_chunks_per_region, num_workers);

    _chunk_bitmap.resize(_num_chunks_per_region * _num_evac_fail_regions);
  }
};

#endif // SHARE_GC_G1_G1EVACFAILURE_HPP