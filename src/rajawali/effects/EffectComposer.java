package rajawali.effects;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.opengl.GLES20;
import android.view.Display;
import android.view.WindowManager;

import rajawali.Camera2D;
import rajawali.materials.TextureManager.FilterType;
import rajawali.materials.TextureManager.WrapType;
import rajawali.primitives.Plane;
import rajawali.renderer.RajawaliRenderer;
import rajawali.renderer.RenderTarget;

public class EffectComposer {
	protected RajawaliRenderer mRenderer;
	protected RenderTarget mRenderTarget1;
	protected RenderTarget mRenderTarget2;
	protected RenderTarget mReadBuffer;
	protected RenderTarget mWriteBuffer;
	
	protected List<APass> mPasses;
	
	protected ShaderPass mCopyPass;
	
	protected Camera2D mCamera = new Camera2D();
	protected Plane mPostProcessingQuad = new Plane(1, 1, 1, 1);
	
	public EffectComposer(RajawaliRenderer renderer, RenderTarget renderTarget) {
		mRenderer = renderer;
		if (renderTarget == null) {
			int width, height;
			if (renderer.getSceneInitialized()) {
				width = mRenderer.getViewportWidth();
				height = mRenderer.getViewportHeight();
			} else {
				WindowManager wm = (WindowManager)renderer.getContext()
						.getSystemService(Context.WINDOW_SERVICE);
				Display display = wm.getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);
				width = size.x;
				height = size.y;
			}
			mRenderTarget1 = new RenderTarget(width, height, 0, 0, true, 
					false, true, GLES20.GL_UNSIGNED_BYTE, Config.RGB_565,
					FilterType.LINEAR, WrapType.CLAMP);
		} else {
			mRenderTarget1 = renderTarget;
		}
		
		mRenderTarget2 = mRenderTarget1.clone();
		
		mWriteBuffer = mRenderTarget1;
		mReadBuffer = mRenderTarget2;
		
		mPasses = Collections.synchronizedList(new CopyOnWriteArrayList<APass>());
		
		mCamera.setProjectionMatrix(0, 0);
	}
	
	/**
	 * Swaps read and write buffers.
	 */
	public void swapBuffers() {
		RenderTarget tmp = mReadBuffer;
		mReadBuffer = mWriteBuffer;
		mWriteBuffer = tmp;
	}
	
	public void addPass(APass pass) {
		mPasses.add(pass);
	}
	
	public void insertPass(APass pass, int index) {
		mPasses.add(index, pass);
	}
	
	public void removePass(APass pass) {
		mPasses.remove(pass);
	}
	
	public void setSize(int width, int height) {
		RenderTarget renderTarget = mRenderTarget1.clone();
		renderTarget.setWidth(width);
		renderTarget.setHeight(height);
		reset(renderTarget);
	}
	
	public void reset(RenderTarget renderTarget) {
		if (renderTarget == null) {
			int width, height;
			if (mRenderer.getSceneInitialized()) {
				width = mRenderer.getViewportWidth();
				height = mRenderer.getViewportHeight();
			} else {
				WindowManager wm = (WindowManager)mRenderer.getContext()
						.getSystemService(Context.WINDOW_SERVICE);
				Display display = wm.getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);
				width = size.x;
				height = size.y;
			}
			mRenderTarget1 = new RenderTarget(width, height, 0, 0, true, 
					false, true, GLES20.GL_UNSIGNED_BYTE, Config.RGB_565,
					FilterType.LINEAR, WrapType.CLAMP);
		} else  {
			mRenderTarget1 = renderTarget;
		}
		
		mRenderTarget2 = mRenderTarget1.clone();
		mWriteBuffer = mRenderTarget1;
		mReadBuffer = mRenderTarget2;
	}
	
	public void render(double deltaTime) {
		mWriteBuffer = mRenderTarget1;
		mReadBuffer = mRenderTarget2;
		
		boolean maskActive = false;
		
		APass pass;
		
		for (int i = 0; i < mPasses.size(); i++) {
			pass = mPasses.get(i);
			if (!pass.isEnabled()) continue;
			
			pass.render(mRenderer, mWriteBuffer, mReadBuffer, deltaTime);
			
			if (pass.needsSwap()) {
				if (maskActive) {
					GLES20.glStencilFunc(GLES20.GL_NOTEQUAL, 1, 0xffffffff);
					
					// TODO: Add ShaderPass stuff here
					// mCopyPass.render(mRenderer, mWriteBuffer, mReadBuffer, deltaTime);
					
					GLES20.glStencilFunc(GLES20.GL_EQUAL, 1, 0xffffffff);
				}
				
				swapBuffers();
			}
			
			//if (pass instanceof MaskPass) {
			//} else if (pass instanceof ClearMaskPass) {
			//}
		}
	}
}
