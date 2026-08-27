import { Component, ElementRef, ViewChild, AfterViewInit, OnDestroy, HostListener, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as THREE from 'three';

@Component({
  selector: 'app-three-bg',
  standalone: true,
  imports: [CommonModule],
  template: `<div #canvasContainer class="three-canvas-container"></div>`,
  styles: [`
    .three-canvas-container {
      position: fixed;
      top: 0;
      left: 0;
      width: 100vw;
      height: 100vh;
      z-index: 0;
      pointer-events: none;
      overflow: hidden;
      background: radial-gradient(circle at 50% 30%, #1e3a8a 0%, #0f172a 70%, #020617 100%);
    }
  `]
})
export class ThreeBgComponent implements AfterViewInit, OnDestroy {
  @ViewChild('canvasContainer', { static: true }) containerRef!: ElementRef<HTMLDivElement>;

  private scene!: THREE.Scene;
  private camera!: THREE.PerspectiveCamera;
  private renderer!: THREE.WebGLRenderer;
  private animFrameId: number | null = null;

  // 3D Objects
  private floatingGroup = new THREE.Group();
  private meshItems: { mesh: THREE.Mesh | THREE.LineSegments; rotSpeedX: number; rotSpeedY: number; floatSpeed: number; initialY: number }[] = [];
  private particlesMesh!: THREE.Points;

  // Mouse & Parallax state
  private mouseX = 0;
  private mouseY = 0;
  private targetMouseX = 0;
  private targetMouseY = 0;

  // Point light for interactive mouse glow
  private mouseLight!: THREE.PointLight;

  ngAfterViewInit(): void {
    this.initThree();
    this.createSceneContent();
    this.animate();
  }

  ngOnDestroy(): void {
    if (this.animFrameId !== null) {
      cancelAnimationFrame(this.animFrameId);
    }
    if (this.renderer) {
      this.renderer.dispose();
      if (this.containerRef?.nativeElement && this.renderer.domElement) {
        this.containerRef.nativeElement.removeChild(this.renderer.domElement);
      }
    }
  }

  @HostListener('window:mousemove', ['$event'])
  onMouseMove(event: MouseEvent): void {
    const halfWidth = window.innerWidth / 2;
    const halfHeight = window.innerHeight / 2;
    this.targetMouseX = (event.clientX - halfWidth) / halfWidth;
    this.targetMouseY = (event.clientY - halfHeight) / halfHeight;
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    if (!this.camera || !this.renderer || !this.containerRef) return;
    const width = window.innerWidth;
    const height = window.innerHeight;
    this.camera.aspect = width / height;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(width, height);
  }

  private initThree(): void {
    const container = this.containerRef.nativeElement;
    const width = window.innerWidth;
    const height = window.innerHeight;

    // Scene setup
    this.scene = new THREE.Scene();
    this.scene.fog = new THREE.FogExp2(0x0f172a, 0.035);

    // Camera setup
    this.camera = new THREE.PerspectiveCamera(60, width / height, 0.1, 1000);
    this.camera.position.z = 18;

    // Renderer setup
    this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    this.renderer.setSize(width, height);
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
    container.appendChild(this.renderer.domElement);

    // Lighting using brand palette: Primary (#1e3a8a), Secondary (#0f172a), Accent (#06b6d4), Amber (#f59e0b)
    const ambientLight = new THREE.AmbientLight(0x1e3a8a, 1.2);
    this.scene.add(ambientLight);

    const dirLight1 = new THREE.DirectionalLight(0x06b6d4, 2.5);
    dirLight1.position.set(20, 20, 20);
    this.scene.add(dirLight1);

    const dirLight2 = new THREE.DirectionalLight(0xf59e0b, 1.5);
    dirLight2.position.set(-20, -10, 10);
    this.scene.add(dirLight2);

    // Interactive mouse point light
    this.mouseLight = new THREE.PointLight(0x38bdf8, 3, 25);
    this.scene.add(this.mouseLight);

    this.scene.add(this.floatingGroup);
  }

  private createSceneContent(): void {
    // 1. Particle Constellation Field
    const particleCount = 300;
    const geometry = new THREE.BufferGeometry();
    const positions = new Float32Array(particleCount * 3);
    const colors = new Float32Array(particleCount * 3);

    const color1 = new THREE.Color(0x06b6d4); // Cyan accent
    const color2 = new THREE.Color(0x3b82f6); // Blue primary
    const color3 = new THREE.Color(0xf59e0b); // Amber gold

    for (let i = 0; i < particleCount; i++) {
      positions[i * 3] = (Math.random() - 0.5) * 50;
      positions[i * 3 + 1] = (Math.random() - 0.5) * 50;
      positions[i * 3 + 2] = (Math.random() - 0.5) * 30;

      const mixColor = Math.random() < 0.5 ? color1 : (Math.random() < 0.8 ? color2 : color3);
      colors[i * 3] = mixColor.r;
      colors[i * 3 + 1] = mixColor.g;
      colors[i * 3 + 2] = mixColor.b;
    }

    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3));

    const particleMaterial = new THREE.PointsMaterial({
      size: 0.25,
      vertexColors: true,
      transparent: true,
      opacity: 0.75,
      blending: THREE.AdditiveBlending
    });

    this.particlesMesh = new THREE.Points(geometry, particleMaterial);
    this.scene.add(this.particlesMesh);

    // 2. Central Glossy 3D Geometries (Bank / Vault themed polyhedrons)
    // Metallic Material matching brand dark & blue glossy sheen
    const glassMat = new THREE.MeshStandardMaterial({
      color: 0x1e3a8a,
      metalness: 0.8,
      roughness: 0.2,
      wireframe: false,
      transparent: true,
      opacity: 0.65
    });

    const wireMat = new THREE.MeshBasicMaterial({
      color: 0x38bdf8,
      wireframe: true,
      transparent: true,
      opacity: 0.35
    });

    const goldMat = new THREE.MeshStandardMaterial({
      color: 0xf59e0b,
      metalness: 0.9,
      roughness: 0.1,
      transparent: true,
      opacity: 0.7
    });

    // Outer Icosahedron
    const icoGeo = new THREE.IcosahedronGeometry(4, 0);
    const icoMesh = new THREE.Mesh(icoGeo, glassMat);
    const icoWire = new THREE.Mesh(icoGeo, wireMat);
    icoMesh.add(icoWire);
    icoMesh.position.set(-6, 2, -2);
    this.floatingGroup.add(icoMesh);
    this.meshItems.push({ mesh: icoMesh, rotSpeedX: 0.003, rotSpeedY: 0.005, floatSpeed: 0.001, initialY: 2 });

    // Floating Torus Ring (Bank Token Ring)
    const torusGeo = new THREE.TorusGeometry(3.5, 0.4, 16, 100);
    const torusMesh = new THREE.Mesh(torusGeo, goldMat);
    torusMesh.position.set(7, -3, -1);
    torusMesh.rotation.x = Math.PI / 3;
    this.floatingGroup.add(torusMesh);
    this.meshItems.push({ mesh: torusMesh, rotSpeedX: 0.004, rotSpeedY: 0.002, floatSpeed: 0.0012, initialY: -3 });

    // Small Floating Octahedrons
    const octGeo = new THREE.OctahedronGeometry(1.8, 0);
    for (let i = 0; i < 4; i++) {
      const octMat = i % 2 === 0 ? glassMat : goldMat;
      const octMesh = new THREE.Mesh(octGeo, octMat);
      const posX = (Math.random() - 0.5) * 30;
      const posY = (Math.random() - 0.5) * 20;
      const posZ = (Math.random() - 0.5) * 15;
      octMesh.position.set(posX, posY, posZ);
      this.floatingGroup.add(octMesh);
      this.meshItems.push({
        mesh: octMesh,
        rotSpeedX: (Math.random() - 0.5) * 0.01,
        rotSpeedY: (Math.random() - 0.5) * 0.01,
        floatSpeed: 0.001 + Math.random() * 0.001,
        initialY: posY
      });
    }
  }

  private animate = (): void => {
    this.animFrameId = requestAnimationFrame(this.animate);

    // Smooth mouse interpolation (lerp)
    this.mouseX += (this.targetMouseX - this.mouseX) * 0.05;
    this.mouseY += (this.targetMouseY - this.mouseY) * 0.05;

    // Update mouse point light position in 3D scene
    if (this.mouseLight) {
      this.mouseLight.position.x = this.mouseX * 15;
      this.mouseLight.position.y = -this.mouseY * 15;
      this.mouseLight.position.z = 8;
    }

    // Camera parallax effect
    if (this.camera) {
      this.camera.position.x = this.mouseX * 3;
      this.camera.position.y = -this.mouseY * 3;
      this.camera.lookAt(this.scene.position);
    }

    // Rotate particle field slowly
    if (this.particlesMesh) {
      this.particlesMesh.rotation.y += 0.0005;
      this.particlesMesh.rotation.x = this.mouseY * 0.1;
    }

    // Animate individual floating meshes
    const time = Date.now() * 0.001;
    this.meshItems.forEach((item, index) => {
      item.mesh.rotation.x += item.rotSpeedX;
      item.mesh.rotation.y += item.rotSpeedY;
      item.mesh.position.y = item.initialY + Math.sin(time + index) * 0.6;
    });

    this.renderer.render(this.scene, this.camera);
  };
}
